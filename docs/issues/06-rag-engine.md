# Issue #06: RAG Engine & Knowledge System

## 🎯 Epic: Retrieval-Augmented Generation
**Priority**: P2 (Medium)  
**Estimate**: 10-12 days  
**Dependencies**: #01 (Core Architecture), #04 (Model Management), #05 (Chat Engine)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 6 RAG Engine

## 📋 Overview
Implement a comprehensive RAG (Retrieval-Augmented Generation) system that enables contextual knowledge retrieval from local documents, with support for multiple document formats, efficient embeddings generation, vector storage, and semantic search capabilities.

## 🎯 Goals
- **Document Processing**: Support for PDF, TXT, MD, DOCX, and other formats
- **Vector Storage**: Efficient local vector database for embeddings
- **Semantic Search**: Fast and accurate context retrieval
- **Knowledge Management**: Document organization and metadata handling
- **Context Integration**: Seamless integration with chat engine
- **Privacy-First**: All processing remains on-device

## 📝 Detailed Tasks

### 1. Document Processing Pipeline

#### 1.1 Document Ingestion System
Create `core-rag/src/main/kotlin/DocumentProcessorImpl.kt`:

```kotlin
@Singleton
class DocumentProcessorImpl @Inject constructor(
    private val textExtractor: TextExtractor,
    private val chunkingService: ChunkingService,
    private val embeddingService: EmbeddingService,
    private val vectorStore: VectorStore,
    private val eventBus: EventBus,
    @ApplicationContext private val context: Context
) : DocumentProcessor {
    
    companion object {
        private const val TAG = "DocumentProcessor"
        private const val MAX_CHUNK_SIZE = 1000
        private const val CHUNK_OVERLAP = 200
        private const val SUPPORTED_MIME_TYPES = "application/pdf,text/plain,text/markdown,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }
    
    override suspend fun processDocument(
        uri: Uri,
        metadata: DocumentMetadata?
    ): Result<ProcessingResult> = withContext(Dispatchers.IO) {
        
        try {
            Log.i(TAG, "Starting document processing for: $uri")
            eventBus.emit(IrisEvent.DocumentProcessingStarted(uri.toString()))
            
            val startTime = System.currentTimeMillis()
            
            // Extract document info and validate
            val documentInfo = extractDocumentInfo(uri)
            if (!isSupportedFormat(documentInfo.mimeType)) {
                return@withContext Result.failure(
                    DocumentProcessingException("Unsupported file format: ${documentInfo.mimeType}")
                )
            }
            
            // Extract text content
            val extractionResult = textExtractor.extractText(uri, documentInfo)
            if (extractionResult.isFailure) {
                return@withContext Result.failure(
                    extractionResult.exceptionOrNull() ?: Exception("Text extraction failed")
                )
            }
            
            val textContent = extractionResult.getOrNull()!!
            Log.d(TAG, "Extracted ${textContent.length} characters from document")
            
            // Create document record
            val document = Document(
                id = generateDocumentId(),
                title = metadata?.title ?: documentInfo.fileName,
                uri = uri.toString(),
                mimeType = documentInfo.mimeType,
                size = documentInfo.size,
                textContent = textContent,
                metadata = metadata?.properties ?: emptyMap(),
                createdAt = System.currentTimeMillis(),
                lastModified = documentInfo.lastModified,
                chunkCount = 0,
                isProcessed = false
            )
            
            // Save document record
            vectorStore.saveDocument(document)
            
            // Chunk the document
            val chunks = chunkingService.chunkText(
                text = textContent,
                maxChunkSize = MAX_CHUNK_SIZE,
                overlap = CHUNK_OVERLAP,
                documentId = document.id
            )
            
            Log.d(TAG, "Created ${chunks.size} chunks from document")
            
            // Generate embeddings for chunks
            val embeddedChunks = generateEmbeddingsForChunks(chunks, document.id)
            
            // Store chunks and embeddings
            vectorStore.saveChunks(embeddedChunks)
            
            // Update document record
            val updatedDocument = document.copy(
                chunkCount = chunks.size,
                isProcessed = true
            )
            vectorStore.updateDocument(updatedDocument)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            val result = ProcessingResult(
                documentId = document.id,
                title = document.title,
                chunkCount = chunks.size,
                characterCount = textContent.length,
                processingTime = processingTime,
                success = true
            )
            
            Log.i(TAG, "Document processed successfully: ${document.id}")
            eventBus.emit(IrisEvent.DocumentProcessingCompleted(document.id, result))
            
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Document processing failed", e)
            eventBus.emit(IrisEvent.DocumentProcessingFailed(uri.toString(), e.message ?: "Unknown error"))
            Result.failure(DocumentProcessingException("Document processing failed", e))
        }
    }
    
    override suspend fun processMultipleDocuments(
        uris: List<Uri>,
        batchMetadata: Map<String, DocumentMetadata>?
    ): Flow<BatchProcessingResult> = flow {
        
        val totalDocuments = uris.size
        var processedCount = 0
        var successCount = 0
        val errors = mutableListOf<ProcessingError>()
        
        emit(BatchProcessingResult.Started(totalDocuments))
        
        uris.forEach { uri ->
            try {
                val metadata = batchMetadata?.get(uri.toString())
                val result = processDocument(uri, metadata)
                
                processedCount++
                
                if (result.isSuccess) {
                    successCount++
                    emit(BatchProcessingResult.DocumentCompleted(
                        uri = uri,
                        result = result.getOrNull()!!,
                        progress = (processedCount * 100) / totalDocuments
                    ))
                } else {
                    val error = ProcessingError(
                        uri = uri,
                        error = result.exceptionOrNull()?.message ?: "Processing failed"
                    )
                    errors.add(error)
                    emit(BatchProcessingResult.DocumentFailed(error))
                }
                
            } catch (e: Exception) {
                val error = ProcessingError(
                    uri = uri,
                    error = e.message ?: "Unknown error"
                )
                errors.add(error)
                emit(BatchProcessingResult.DocumentFailed(error))
                processedCount++
            }
        }
        
        emit(BatchProcessingResult.Completed(
            totalDocuments = totalDocuments,
            successCount = successCount,
            errorCount = errors.size,
            errors = errors
        ))
    }
    
    override suspend fun reprocessDocument(documentId: String): Result<ProcessingResult> {
        return try {
            val document = vectorStore.getDocument(documentId)
                ?: return Result.failure(DocumentProcessingException("Document not found: $documentId"))
            
            // Delete existing chunks
            vectorStore.deleteChunksByDocumentId(documentId)
            
            // Reprocess from URI
            val uri = Uri.parse(document.uri)
            processDocument(uri, DocumentMetadata(
                title = document.title,
                properties = document.metadata
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Document reprocessing failed", e)
            Result.failure(DocumentProcessingException("Reprocessing failed", e))
        }
    }
    
    override suspend fun deleteDocument(documentId: String): Boolean {
        return try {
            vectorStore.deleteDocument(documentId)
            vectorStore.deleteChunksByDocumentId(documentId)
            
            eventBus.emit(IrisEvent.DocumentDeleted(documentId))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete document", e)
            false
        }
    }
    
    override suspend fun getDocumentStatus(documentId: String): DocumentStatus? {
        return try {
            val document = vectorStore.getDocument(documentId) ?: return null
            
            DocumentStatus(
                documentId = document.id,
                title = document.title,
                isProcessed = document.isProcessed,
                chunkCount = document.chunkCount,
                size = document.size,
                createdAt = document.createdAt,
                lastModified = document.lastModified
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get document status", e)
            null
        }
    }
    
    override suspend fun getAllDocuments(): List<DocumentStatus> {
        return try {
            vectorStore.getAllDocuments().map { document ->
                DocumentStatus(
                    documentId = document.id,
                    title = document.title,
                    isProcessed = document.isProcessed,
                    chunkCount = document.chunkCount,
                    size = document.size,
                    createdAt = document.createdAt,
                    lastModified = document.lastModified
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all documents", e)
            emptyList()
        }
    }
    
    // Private helper methods
    
    private suspend fun extractDocumentInfo(uri: Uri): DocumentInfo {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                
                val fileName = if (nameIndex >= 0) cursor.getString(nameIndex) else "Unknown"
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                
                // Get MIME type
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                
                DocumentInfo(
                    fileName = fileName,
                    size = size,
                    mimeType = mimeType,
                    lastModified = System.currentTimeMillis() // Fallback
                )
            } else {
                throw DocumentProcessingException("Could not read document info")
            }
        } ?: throw DocumentProcessingException("Could not access document")
    }
    
    private fun isSupportedFormat(mimeType: String): Boolean {
        return SUPPORTED_MIME_TYPES.split(",").any { supportedType ->
            mimeType.startsWith(supportedType.trim())
        }
    }
    
    private suspend fun generateEmbeddingsForChunks(
        chunks: List<DocumentChunk>,
        documentId: String
    ): List<EmbeddedChunk> {
        return chunks.mapIndexed { index, chunk ->
            try {
                val embedding = embeddingService.generateEmbedding(chunk.content)
                
                EmbeddedChunk(
                    id = "${documentId}_chunk_${index}",
                    documentId = documentId,
                    content = chunk.content,
                    embedding = embedding,
                    startIndex = chunk.startIndex,
                    endIndex = chunk.endIndex,
                    metadata = chunk.metadata
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to generate embedding for chunk $index", e)
                // Return chunk with empty embedding as fallback
                EmbeddedChunk(
                    id = "${documentId}_chunk_${index}",
                    documentId = documentId,
                    content = chunk.content,
                    embedding = FloatArray(384), // Empty embedding
                    startIndex = chunk.startIndex,
                    endIndex = chunk.endIndex,
                    metadata = chunk.metadata
                )
            }
        }
    }
    
    private fun generateDocumentId(): String {
        return "doc_${System.currentTimeMillis()}_${(100000..999999).random()}"
    }
}

// Text extraction implementations
@Singleton
class TextExtractorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TextExtractor {
    
    companion object {
        private const val TAG = "TextExtractor"
        private const val MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB
    }
    
    override suspend fun extractText(uri: Uri, documentInfo: DocumentInfo): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (documentInfo.size > MAX_FILE_SIZE) {
                    return@withContext Result.failure(
                        DocumentProcessingException("File too large: ${documentInfo.size} bytes")
                    )
                }
                
                when {
                    documentInfo.mimeType.startsWith("text/") -> extractPlainText(uri)
                    documentInfo.mimeType == "application/pdf" -> extractPdfText(uri)
                    documentInfo.mimeType.contains("wordprocessingml") -> extractDocxText(uri)
                    else -> Result.failure(
                        DocumentProcessingException("Unsupported format: ${documentInfo.mimeType}")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Text extraction failed", e)
                Result.failure(DocumentProcessingException("Text extraction failed", e))
            }
        }
    }
    
    private suspend fun extractPlainText(uri: Uri): Result<String> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val text = inputStream.bufferedReader().use { it.readText() }
                Result.success(text)
            } ?: Result.failure(DocumentProcessingException("Could not open file"))
        } catch (e: Exception) {
            Result.failure(DocumentProcessingException("Plain text extraction failed", e))
        }
    }
    
    private suspend fun extractPdfText(uri: Uri): Result<String> {
        return try {
            // Note: In a real implementation, you would use a PDF library like Apache PDFBox
            // For now, return a placeholder
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // TODO: Implement PDF text extraction
                // This would typically use a library like:
                // - Apache PDFBox (large dependency)
                // - MuPDF (native library)
                // - iText (commercial license considerations)
                
                Result.success("PDF text extraction not yet implemented")
            } ?: Result.failure(DocumentProcessingException("Could not open PDF file"))
        } catch (e: Exception) {
            Result.failure(DocumentProcessingException("PDF extraction failed", e))
        }
    }
    
    private suspend fun extractDocxText(uri: Uri): Result<String> {
        return try {
            // Note: In a real implementation, you would use a library like Apache POI
            // For now, return a placeholder
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // TODO: Implement DOCX text extraction
                // This would typically use Apache POI or similar library
                
                Result.success("DOCX text extraction not yet implemented")
            } ?: Result.failure(DocumentProcessingException("Could not open DOCX file"))
        } catch (e: Exception) {
            Result.failure(DocumentProcessingException("DOCX extraction failed", e))
        }
    }
}

// Text chunking implementation
@Singleton
class ChunkingServiceImpl @Inject constructor() : ChunkingService {
    
    companion object {
        private const val TAG = "ChunkingService"
    }
    
    override suspend fun chunkText(
        text: String,
        maxChunkSize: Int,
        overlap: Int,
        documentId: String
    ): List<DocumentChunk> = withContext(Dispatchers.Default) {
        
        val chunks = mutableListOf<DocumentChunk>()
        
        // Split by paragraphs first, then by sentences if needed
        val paragraphs = text.split("\n\n", "\n").filter { it.isNotBlank() }
        
        var currentChunk = StringBuilder()
        var chunkStartIndex = 0
        var currentIndex = 0
        
        for (paragraph in paragraphs) {
            val cleanParagraph = paragraph.trim()
            
            // If adding this paragraph would exceed chunk size, finalize current chunk
            if (currentChunk.isNotEmpty() && 
                currentChunk.length + cleanParagraph.length + 1 > maxChunkSize) {
                
                // Create chunk
                val chunkContent = currentChunk.toString().trim()
                if (chunkContent.isNotEmpty()) {
                    chunks.add(DocumentChunk(
                        content = chunkContent,
                        startIndex = chunkStartIndex,
                        endIndex = chunkStartIndex + chunkContent.length,
                        metadata = mapOf(
                            "chunk_index" to chunks.size.toString(),
                            "document_id" to documentId
                        )
                    ))
                }
                
                // Start new chunk with overlap
                val overlapText = if (overlap > 0 && currentChunk.length > overlap) {
                    currentChunk.substring(currentChunk.length - overlap)
                } else ""
                
                currentChunk = StringBuilder(overlapText)
                chunkStartIndex = currentIndex - overlapText.length
            }
            
            // Add paragraph to current chunk
            if (currentChunk.isNotEmpty()) {
                currentChunk.append("\n\n")
            }
            currentChunk.append(cleanParagraph)
            currentIndex += cleanParagraph.length + 2 // +2 for line breaks
        }
        
        // Add final chunk
        val finalChunkContent = currentChunk.toString().trim()
        if (finalChunkContent.isNotEmpty()) {
            chunks.add(DocumentChunk(
                content = finalChunkContent,
                startIndex = chunkStartIndex,
                endIndex = chunkStartIndex + finalChunkContent.length,
                metadata = mapOf(
                    "chunk_index" to chunks.size.toString(),
                    "document_id" to documentId
                )
            ))
        }
        
        Log.d(TAG, "Created ${chunks.size} chunks from ${text.length} characters")
        chunks
    }
    
    override suspend fun smartChunkText(
        text: String,
        maxChunkSize: Int,
        overlap: Int,
        documentId: String
    ): List<DocumentChunk> = withContext(Dispatchers.Default) {
        
        // More sophisticated chunking that respects semantic boundaries
        val sentences = splitIntoSentences(text)
        val chunks = mutableListOf<DocumentChunk>()
        
        var currentChunk = StringBuilder()
        var chunkStartIndex = 0
        var currentIndex = 0
        
        for (sentence in sentences) {
            val cleanSentence = sentence.trim()
            
            // Check if adding this sentence would exceed chunk size
            if (currentChunk.isNotEmpty() && 
                currentChunk.length + cleanSentence.length + 1 > maxChunkSize) {
                
                // Finalize current chunk
                val chunkContent = currentChunk.toString().trim()
                if (chunkContent.isNotEmpty()) {
                    chunks.add(DocumentChunk(
                        content = chunkContent,
                        startIndex = chunkStartIndex,
                        endIndex = chunkStartIndex + chunkContent.length,
                        metadata = mapOf(
                            "chunk_index" to chunks.size.toString(),
                            "document_id" to documentId,
                            "chunking_method" to "smart_semantic"
                        )
                    ))
                }
                
                // Start new chunk with overlap
                val overlapText = if (overlap > 0) {
                    getLastNCharacters(currentChunk.toString(), overlap)
                } else ""
                
                currentChunk = StringBuilder(overlapText)
                chunkStartIndex = currentIndex - overlapText.length
            }
            
            // Add sentence to current chunk
            if (currentChunk.isNotEmpty()) {
                currentChunk.append(" ")
            }
            currentChunk.append(cleanSentence)
            currentIndex += cleanSentence.length + 1
        }
        
        // Add final chunk
        val finalChunkContent = currentChunk.toString().trim()
        if (finalChunkContent.isNotEmpty()) {
            chunks.add(DocumentChunk(
                content = finalChunkContent,
                startIndex = chunkStartIndex,
                endIndex = chunkStartIndex + finalChunkContent.length,
                metadata = mapOf(
                    "chunk_index" to chunks.size.toString(),
                    "document_id" to documentId,
                    "chunking_method" to "smart_semantic"
                )
            ))
        }
        
        Log.d(TAG, "Smart chunking created ${chunks.size} chunks from ${sentences.size} sentences")
        chunks
    }
    
    private fun splitIntoSentences(text: String): List<String> {
        // Simple sentence splitting - in a real implementation, 
        // you might use a more sophisticated NLP library
        return text.split(Regex("[.!?]+\\s+"))
            .filter { it.isNotBlank() }
            .map { it.trim() }
    }
    
    private fun getLastNCharacters(text: String, n: Int): String {
        return if (text.length <= n) text else text.substring(text.length - n)
    }
}

// Data classes for document processing
data class DocumentInfo(
    val fileName: String,
    val size: Long,
    val mimeType: String,
    val lastModified: Long
)

data class Document(
    val id: String,
    val title: String,
    val uri: String,
    val mimeType: String,
    val size: Long,
    val textContent: String,
    val metadata: Map<String, String>,
    val createdAt: Long,
    val lastModified: Long,
    val chunkCount: Int,
    val isProcessed: Boolean
)

data class DocumentChunk(
    val content: String,
    val startIndex: Int,
    val endIndex: Int,
    val metadata: Map<String, String>
)

data class EmbeddedChunk(
    val id: String,
    val documentId: String,
    val content: String,
    val embedding: FloatArray,
    val startIndex: Int,
    val endIndex: Int,
    val metadata: Map<String, String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EmbeddedChunk
        
        if (id != other.id) return false
        if (documentId != other.documentId) return false
        if (content != other.content) return false
        if (!embedding.contentEquals(other.embedding)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}

data class DocumentMetadata(
    val title: String? = null,
    val author: String? = null,
    val tags: List<String> = emptyList(),
    val properties: Map<String, String> = emptyMap()
)

data class ProcessingResult(
    val documentId: String,
    val title: String,
    val chunkCount: Int,
    val characterCount: Int,
    val processingTime: Long,
    val success: Boolean
)

data class DocumentStatus(
    val documentId: String,
    val title: String,
    val isProcessed: Boolean,
    val chunkCount: Int,
    val size: Long,
    val createdAt: Long,
    val lastModified: Long
)

data class ProcessingError(
    val uri: Uri,
    val error: String
)

// Batch processing results
sealed class BatchProcessingResult {
    data class Started(val totalDocuments: Int) : BatchProcessingResult()
    data class DocumentCompleted(
        val uri: Uri,
        val result: ProcessingResult,
        val progress: Int
    ) : BatchProcessingResult()
    data class DocumentFailed(val error: ProcessingError) : BatchProcessingResult()
    data class Completed(
        val totalDocuments: Int,
        val successCount: Int,
        val errorCount: Int,
        val errors: List<ProcessingError>
    ) : BatchProcessingResult()
}

class DocumentProcessingException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

### 2. Vector Storage & Retrieval System

#### 2.1 Vector Store Implementation
Create `core-rag/src/main/kotlin/VectorStoreImpl.kt`:

```kotlin
@Singleton
class VectorStoreImpl @Inject constructor(
    private val database: IrisDatabase,
    private val embeddingService: EmbeddingService,
    @ApplicationContext private val context: Context
) : VectorStore {
    
    companion object {
        private const val TAG = "VectorStore"
        private const val DEFAULT_TOP_K = 10
        private const val SIMILARITY_THRESHOLD = 0.7f
        private const val MAX_RESULTS = 50
    }
    
    override suspend fun saveDocument(document: Document) {
        database.documentDao().insertDocument(document.toEntity())
    }
    
    override suspend fun saveChunks(chunks: List<EmbeddedChunk>) {
        val entities = chunks.map { it.toEntity() }
        database.chunkDao().insertChunks(entities)
    }
    
    override suspend fun updateDocument(document: Document) {
        database.documentDao().updateDocument(document.toEntity())
    }
    
    override suspend fun getDocument(documentId: String): Document? {
        return database.documentDao().getDocumentById(documentId)?.toDomain()
    }
    
    override suspend fun getAllDocuments(): List<Document> {
        return database.documentDao().getAllDocuments().map { it.toDomain() }
    }
    
    override suspend fun deleteDocument(documentId: String) {
        database.documentDao().deleteDocument(documentId)
    }
    
    override suspend fun deleteChunksByDocumentId(documentId: String) {
        database.chunkDao().deleteChunksByDocumentId(documentId)
    }
    
    override suspend fun searchSimilar(
        query: String,
        topK: Int,
        threshold: Float,
        documentIds: List<String>?
    ): List<SearchResult> = withContext(Dispatchers.Default) {
        
        try {
            // Generate embedding for query
            val queryEmbedding = embeddingService.generateEmbedding(query)
            
            // Get candidate chunks
            val candidateChunks = if (documentIds != null) {
                database.chunkDao().getChunksByDocumentIds(documentIds)
            } else {
                database.chunkDao().getAllChunks()
            }
            
            // Calculate similarities and rank
            val searchResults = candidateChunks.mapNotNull { chunkEntity ->
                val chunk = chunkEntity.toDomain()
                val similarity = calculateCosineSimilarity(queryEmbedding, chunk.embedding)
                
                if (similarity >= threshold) {
                    SearchResult(
                        chunk = chunk,
                        similarity = similarity,
                        relevanceScore = calculateRelevanceScore(similarity, query, chunk.content)
                    )
                } else null
            }
            .sortedByDescending { it.relevanceScore }
            .take(minOf(topK, MAX_RESULTS))
            
            Log.d(TAG, "Found ${searchResults.size} similar chunks for query: ${query.take(50)}...")
            searchResults
            
        } catch (e: Exception) {
            Log.e(TAG, "Vector search failed", e)
            emptyList()
        }
    }
    
    override suspend fun searchByMetadata(
        filters: Map<String, String>,
        limit: Int
    ): List<SearchResult> {
        return try {
            // Note: This is a simplified implementation
            // In a real vector database, you'd have indexed metadata fields
            val allChunks = database.chunkDao().getAllChunks()
            
            val filteredChunks = allChunks.filter { chunkEntity ->
                val chunkMetadata = chunkEntity.metadata
                filters.all { (key, value) ->
                    chunkMetadata[key] == value
                }
            }.take(limit)
            
            filteredChunks.map { chunkEntity ->
                SearchResult(
                    chunk = chunkEntity.toDomain(),
                    similarity = 1.0f, // Perfect match for metadata
                    relevanceScore = 1.0f
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Metadata search failed", e)
            emptyList()
        }
    }
    
    override suspend fun getChunksByDocument(documentId: String): List<EmbeddedChunk> {
        return database.chunkDao().getChunksByDocumentId(documentId).map { it.toDomain() }
    }
    
    override suspend fun getStatistics(): VectorStoreStatistics {
        return try {
            val documentCount = database.documentDao().getDocumentCount()
            val chunkCount = database.chunkDao().getChunkCount()
            val totalSize = database.documentDao().getTotalDocumentSize()
            
            VectorStoreStatistics(
                documentCount = documentCount,
                chunkCount = chunkCount,
                totalSizeBytes = totalSize,
                averageChunksPerDocument = if (documentCount > 0) chunkCount.toFloat() / documentCount else 0f
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get statistics", e)
            VectorStoreStatistics(0, 0, 0L, 0f)
        }
    }
    
    // Private helper methods
    
    private fun calculateCosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size) {
            Log.w(TAG, "Embedding size mismatch: ${embedding1.size} vs ${embedding2.size}")
            return 0f
        }
        
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f
        
        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            norm1 += embedding1[i] * embedding1[i]
            norm2 += embedding2[i] * embedding2[i]
        }
        
        val magnitude = sqrt(norm1) * sqrt(norm2)
        return if (magnitude > 0) dotProduct / magnitude else 0f
    }
    
    private fun calculateRelevanceScore(
        similarity: Float,
        query: String,
        content: String
    ): Float {
        // Combine cosine similarity with additional relevance factors
        var score = similarity
        
        // Boost score for exact keyword matches
        val queryWords = query.lowercase().split("\\s+".toRegex())
        val contentWords = content.lowercase().split("\\s+".toRegex())
        
        val keywordMatches = queryWords.count { queryWord ->
            contentWords.any { contentWord ->
                contentWord.contains(queryWord) || queryWord.contains(contentWord)
            }
        }
        
        val keywordBoost = (keywordMatches.toFloat() / queryWords.size) * 0.2f
        score += keywordBoost
        
        // Boost for content length (prefer substantial chunks)
        val lengthBoost = minOf(content.length / 1000f, 0.1f)
        score += lengthBoost
        
        return minOf(score, 1.0f)
    }
}

// Embedding service implementation
@Singleton
class EmbeddingServiceImpl @Inject constructor(
    private val nativeEngine: NativeInferenceEngine,
    private val modelRegistry: ModelRegistry
) : EmbeddingService {
    
    companion object {
        private const val TAG = "EmbeddingService"
        private const val DEFAULT_EMBEDDING_MODEL = "all-minilm-l6-v2-q8_0"
    }
    
    private var isEmbeddingModelLoaded = false
    private var currentEmbeddingModel: String? = null
    
    override suspend fun generateEmbedding(text: String): FloatArray {
        return withContext(Dispatchers.IO) {
            try {
                ensureEmbeddingModelLoaded()
                
                // Clean and prepare text
                val cleanText = text.trim().take(512) // Limit to model's max sequence length
                
                // Generate embedding through native engine
                val result = nativeEngine.generateEmbedding(cleanText)
                
                result.getOrThrow()
                
            } catch (e: Exception) {
                Log.e(TAG, "Embedding generation failed for text: ${text.take(50)}...", e)
                throw EmbeddingException("Failed to generate embedding", e)
            }
        }
    }
    
    override suspend fun generateBatchEmbeddings(texts: List<String>): List<FloatArray> {
        return withContext(Dispatchers.IO) {
            texts.map { text ->
                generateEmbedding(text)
            }
        }
    }
    
    override suspend fun loadEmbeddingModel(modelId: String): Result<Unit> {
        return try {
            val model = modelRegistry.getModelById(modelId)
                ?: return Result.failure(EmbeddingException("Embedding model not found: $modelId"))
            
            if (model.type != "embedding") {
                return Result.failure(EmbeddingException("Model is not an embedding model: $modelId"))
            }
            
            // Load model with native engine
            val loadResult = nativeEngine.loadEmbeddingModel(getModelPath(model))
            
            if (loadResult.isSuccess) {
                isEmbeddingModelLoaded = true
                currentEmbeddingModel = modelId
                Log.i(TAG, "Embedding model loaded: $modelId")
            }
            
            loadResult
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load embedding model: $modelId", e)
            Result.failure(EmbeddingException("Model loading failed", e))
        }
    }
    
    override suspend fun unloadEmbeddingModel() {
        try {
            if (isEmbeddingModelLoaded) {
                nativeEngine.unloadEmbeddingModel()
                isEmbeddingModelLoaded = false
                currentEmbeddingModel = null
                Log.i(TAG, "Embedding model unloaded")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error unloading embedding model", e)
        }
    }
    
    override suspend fun isModelLoaded(): Boolean {
        return isEmbeddingModelLoaded
    }
    
    override suspend fun getCurrentModel(): String? {
        return currentEmbeddingModel
    }
    
    private suspend fun ensureEmbeddingModelLoaded() {
        if (!isEmbeddingModelLoaded) {
            val loadResult = loadEmbeddingModel(DEFAULT_EMBEDDING_MODEL)
            if (loadResult.isFailure) {
                throw EmbeddingException("Failed to load default embedding model")
            }
        }
    }
    
    private fun getModelPath(model: ModelDescriptor): String {
        return File(
            File(context.getExternalFilesDir(null), "models"),
            "${model.id}.gguf"
        ).absolutePath
    }
}

// RAG Context Integration
@Singleton
class RAGContextProviderImpl @Inject constructor(
    private val vectorStore: VectorStore,
    private val embeddingService: EmbeddingService
) : RAGContextProvider {
    
    companion object {
        private const val TAG = "RAGContextProvider"
        private const val DEFAULT_CONTEXT_CHUNKS = 3
        private const val MAX_CONTEXT_LENGTH = 2000
    }
    
    override suspend fun getRelevantContext(
        query: String,
        maxChunks: Int,
        documentIds: List<String>?
    ): RAGContext {
        return try {
            val searchResults = vectorStore.searchSimilar(
                query = query,
                topK = maxChunks,
                threshold = 0.6f,
                documentIds = documentIds
            )
            
            val contexts = searchResults.map { result ->
                ContextChunk(
                    content = result.chunk.content,
                    source = getDocumentTitle(result.chunk.documentId),
                    relevanceScore = result.relevanceScore,
                    metadata = result.chunk.metadata
                )
            }
            
            val combinedContext = buildCombinedContext(contexts)
            
            RAGContext(
                query = query,
                contexts = contexts,
                combinedContext = combinedContext,
                totalSources = contexts.map { it.source }.distinct().size,
                retrievalTime = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get relevant context", e)
            RAGContext(
                query = query,
                contexts = emptyList(),
                combinedContext = "",
                totalSources = 0,
                retrievalTime = System.currentTimeMillis()
            )
        }
    }
    
    override suspend fun buildPromptWithContext(
        userPrompt: String,
        context: RAGContext
    ): String {
        if (context.contexts.isEmpty()) {
            return userPrompt
        }
        
        val contextSection = buildString {
            appendLine("Based on the following relevant information:")
            appendLine()
            
            context.contexts.forEachIndexed { index, contextChunk ->
                appendLine("Source ${index + 1} (${contextChunk.source}):")
                appendLine(contextChunk.content.trim())
                appendLine()
            }
            
            appendLine("Please answer the following question:")
            append(userPrompt)
        }
        
        return contextSection
    }
    
    private suspend fun getDocumentTitle(documentId: String): String {
        return try {
            vectorStore.getDocument(documentId)?.title ?: "Unknown Document"
        } catch (e: Exception) {
            "Unknown Document"
        }
    }
    
    private fun buildCombinedContext(contexts: List<ContextChunk>): String {
        return contexts.joinToString("\n\n") { context ->
            "${context.source}: ${context.content}"
        }.take(MAX_CONTEXT_LENGTH)
    }
}

// Data classes for RAG system
data class SearchResult(
    val chunk: EmbeddedChunk,
    val similarity: Float,
    val relevanceScore: Float
)

data class VectorStoreStatistics(
    val documentCount: Int,
    val chunkCount: Int,
    val totalSizeBytes: Long,
    val averageChunksPerDocument: Float
)

data class RAGContext(
    val query: String,
    val contexts: List<ContextChunk>,
    val combinedContext: String,
    val totalSources: Int,
    val retrievalTime: Long
)

data class ContextChunk(
    val content: String,
    val source: String,
    val relevanceScore: Float,
    val metadata: Map<String, String>
)

// Exception classes
class EmbeddingException(message: String, cause: Throwable? = null) : Exception(message, cause)
class RAGException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

## 🧪 Testing Strategy

### Unit Tests
- [ ] **Document Processing**
  - Text extraction from various formats
  - Chunking algorithm correctness
  - Embedding generation accuracy
  - Vector similarity calculations

### Integration Tests
- [ ] **End-to-End RAG Flow**
  - Document upload to context retrieval
  - Multi-document search accuracy
  - Context integration with chat
  - Performance under load

### Performance Tests
- [ ] **Vector Operations**
  - Large document processing speed
  - Search latency benchmarks
  - Memory usage optimization
  - Storage efficiency

### UI Tests
- [ ] **Document Management**
  - Document upload interface
  - Processing progress display
  - Search functionality
  - Context visualization

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Document Support**: PDF, TXT, MD, DOCX processing works correctly
- [ ] **Vector Search**: Fast and accurate semantic search functionality
- [ ] **Context Integration**: Seamless RAG integration with chat engine
- [ ] **Knowledge Management**: Intuitive document organization and metadata
- [ ] **Privacy Preservation**: All processing remains on-device

### Technical Criteria
- [ ] **Processing Speed**: Documents processed in <30 seconds per MB
- [ ] **Search Latency**: Vector search results in <1 second
- [ ] **Storage Efficiency**: Optimized vector storage with compression
- [ ] **Context Quality**: Relevant context retrieval with high accuracy

### User Experience Criteria
- [ ] **Easy Upload**: Drag-and-drop document upload interface
- [ ] **Progress Feedback**: Clear processing progress and status
- [ ] **Search Interface**: Intuitive knowledge search functionality
- [ ] **Context Visibility**: Clear indication of retrieved context in chat

## 🔗 Related Issues
- **Depends on**: #01 (Core Architecture), #04 (Model Management), #05 (Chat Engine)
- **Enables**: #07 (Multimodal Support), #14 (UI/UX Implementation)
- **Related**: #12 (Performance Optimization), #10 (Safety Engine)

## 📋 Definition of Done
- [ ] Complete document processing pipeline for supported formats
- [ ] Efficient vector storage and retrieval system
- [ ] Context integration with chat engine
- [ ] Document management interface
- [ ] Comprehensive test suite covering all scenarios
- [ ] Performance benchmarks meet acceptance criteria
- [ ] Knowledge management UI functional
- [ ] Documentation complete with supported formats and usage
- [ ] Code review completed and approved

---

**Note**: This RAG system provides the foundation for contextual AI responses based on user documents while maintaining full privacy through on-device processing.