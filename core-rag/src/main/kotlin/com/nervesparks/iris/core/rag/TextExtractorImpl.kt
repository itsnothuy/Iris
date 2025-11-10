package com.nervesparks.iris.core.rag

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for extracting text from various document formats
 */
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
            // For now, we'll try to extract basic text if available
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Simple heuristic: try to read as text if it's a text-based PDF
                val bytes = inputStream.readBytes()
                val text = extractTextFromPdfBytes(bytes)
                
                if (text.isNotBlank()) {
                    Result.success(text)
                } else {
                    // PDF is likely image-based or encrypted
                    Result.success("PDF text extraction requires additional libraries. " +
                        "Detected PDF file: ${bytes.size} bytes")
                }
            } ?: Result.failure(DocumentProcessingException("Could not open PDF file"))
        } catch (e: Exception) {
            Result.failure(DocumentProcessingException("PDF extraction failed", e))
        }
    }
    
    private fun extractTextFromPdfBytes(bytes: ByteArray): String {
        // Very basic PDF text extraction
        // This only works for simple, uncompressed text-based PDFs
        val pdfString = String(bytes, Charsets.ISO_8859_1)
        
        // Look for text between stream markers
        val textBuilder = StringBuilder()
        val streamPattern = Regex("stream\\s*(.+?)\\s*endstream", RegexOption.DOT_MATCHES_ALL)
        val matches = streamPattern.findAll(pdfString)
        
        for (match in matches) {
            val streamContent = match.groupValues[1]
            // Try to extract readable text (ASCII printable characters)
            val readableText = streamContent.filter { it.code in 32..126 || it == '\n' }
            if (readableText.length > 10) {
                textBuilder.append(readableText).append("\n")
            }
        }
        
        return textBuilder.toString().trim()
    }
    
    private suspend fun extractDocxText(uri: Uri): Result<String> {
        return try {
            // Note: In a real implementation, you would use a library like Apache POI
            // DOCX files are ZIP archives containing XML files
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // For now, return a placeholder
                val bytes = inputStream.readBytes()
                Result.success("DOCX text extraction requires additional libraries. " +
                    "Detected DOCX file: ${bytes.size} bytes")
            } ?: Result.failure(DocumentProcessingException("Could not open DOCX file"))
        } catch (e: Exception) {
            Result.failure(DocumentProcessingException("DOCX extraction failed", e))
        }
    }
}
