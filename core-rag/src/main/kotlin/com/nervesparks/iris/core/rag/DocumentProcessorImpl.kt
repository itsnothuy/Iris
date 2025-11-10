package com.nervesparks.iris.core.rag

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of document processing pipeline
 */
@Singleton
class DocumentProcessorImpl @Inject constructor(
    private val textExtractor: TextExtractor,
    private val chunkingService: ChunkingService,
    private val embeddingService: EmbeddingService,
    private val vectorStore: VectorStore,
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
            val document = StoredDocument(
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
            
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Document processing failed", e)
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
