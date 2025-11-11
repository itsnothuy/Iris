package com.nervesparks.iris.core.multimodal.image

import android.content.Context
import android.net.Uri
import com.nervesparks.iris.core.multimodal.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mock implementation of ImageProcessor for initial module compilation
 */
class MockImageProcessor(
    private val context: Context
) : ImageProcessor {

    override suspend fun preprocessImage(
        uri: Uri,
        targetSize: Int,
        format: ImageFormat
    ): Result<ProcessedImageData> = withContext(Dispatchers.Default) {
        Result.success(
            ProcessedImageData(
                data = ByteArray(1024), // Mock image data
                format = format,
                width = targetSize,
                height = targetSize,
                channels = 3
            )
        )
    }

    override suspend fun validateImage(
        uri: Uri
    ): Result<Boolean> = withContext(Dispatchers.Default) {
        // Mock validation - always returns true
        Result.success(true)
    }
}