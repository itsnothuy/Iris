package com.nervesparks.iris.core.multimodal.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.nervesparks.iris.core.multimodal.ImageProcessor
import com.nervesparks.iris.core.multimodal.types.ImageFormat
import com.nervesparks.iris.core.multimodal.types.MultimodalInferenceException
import com.nervesparks.iris.core.multimodal.types.ProcessedImageData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of image preprocessing using Android Bitmap APIs
 */
@Singleton
class ImageProcessorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) : ImageProcessor {
    
    companion object {
        private const val TAG = "ImageProcessor"
        private const val MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB
        private const val JPEG_QUALITY = 85
        
        private val SUPPORTED_MIME_TYPES = setOf(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/bmp"
        )
    }
    
    override suspend fun preprocessImage(
        uri: Uri, 
        targetSize: Int, 
        format: ImageFormat
    ): Result<ProcessedImageData> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "Preprocessing image: $uri, targetSize: $targetSize, format: $format")
            
            // Load the original image
            val originalBitmap = loadBitmapFromUri(uri)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Failed to load image from URI: $uri")
                )
            
            // Resize to target dimensions while maintaining aspect ratio
            val resizedBitmap = resizeBitmapToTarget(originalBitmap, targetSize)
            
            // Convert to the requested format and get byte array
            val imageBytes = compressBitmapToBytes(resizedBitmap, format)
            
            val processedData = ProcessedImageData(
                data = imageBytes,
                format = format,
                width = resizedBitmap.width,
                height = resizedBitmap.height,
                channels = when (format) {
                    ImageFormat.PNG -> if (hasAlpha(resizedBitmap)) 4 else 3
                    else -> 3
                }
            )
            
            // Cleanup
            if (originalBitmap != resizedBitmap) {
                originalBitmap.recycle()
            }
            resizedBitmap.recycle()
            
            Log.i(TAG, "Image preprocessed successfully: ${processedData.width}x${processedData.height}, ${imageBytes.size} bytes")
            Result.success(processedData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Image preprocessing failed", e)
            Result.failure(MultimodalInferenceException("Image preprocessing failed", e))
        }
    }
    
    override suspend fun validateImage(uri: Uri): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val contentResolver = context.contentResolver
            
            // Check if URI is accessible
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.success(false)
            
            inputStream.use { stream ->
                // Check file size
                val fileSize = stream.available()
                if (fileSize > MAX_IMAGE_SIZE_BYTES) {
                    Log.w(TAG, "Image too large: $fileSize bytes")
                    return@withContext Result.success(false)
                }
                
                // Check MIME type
                val mimeType = contentResolver.getType(uri)
                if (mimeType == null || !SUPPORTED_MIME_TYPES.contains(mimeType.lowercase())) {
                    Log.w(TAG, "Unsupported MIME type: $mimeType")
                    return@withContext Result.success(false)
                }
                
                // Try to decode the image to verify it's valid
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                
                stream.mark(stream.available())
                BitmapFactory.decodeStream(stream, null, options)
                stream.reset()
                
                val isValid = options.outWidth > 0 && options.outHeight > 0
                
                Log.d(TAG, "Image validation result: $isValid for $uri")
                Result.success(isValid)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Image validation failed", e)
            Result.success(false)
        }
    }
    
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI", e)
            null
        }
    }
    
    private fun resizeBitmapToTarget(bitmap: Bitmap, targetSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // If already smaller than target, return as-is
        if (width <= targetSize && height <= targetSize) {
            return bitmap
        }
        
        // Calculate scale factor to maintain aspect ratio
        val scaleFactor = minOf(
            targetSize.toFloat() / width,
            targetSize.toFloat() / height
        )
        
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun compressBitmapToBytes(bitmap: Bitmap, format: ImageFormat): ByteArray {
        val outputStream = ByteArrayOutputStream()
        
        val compressFormat = when (format) {
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageFormat.WEBP -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            ImageFormat.BMP -> Bitmap.CompressFormat.PNG // BMP not directly supported, use PNG
        }
        
        val quality = when (format) {
            ImageFormat.JPEG -> JPEG_QUALITY
            ImageFormat.WEBP -> JPEG_QUALITY
            else -> 100
        }
        
        bitmap.compress(compressFormat, quality, outputStream)
        return outputStream.toByteArray()
    }
    
    private fun hasAlpha(bitmap: Bitmap): Boolean {
        return bitmap.config == Bitmap.Config.ARGB_8888 && bitmap.hasAlpha()
    }
}
