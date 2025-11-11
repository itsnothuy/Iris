package com.nervesparks.iris.core.multimodal.image

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.nervesparks.iris.core.multimodal.types.ImageFormat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
class ImageProcessorImplTest {
    
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var imageProcessor: ImageProcessorImpl
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        
        imageProcessor = ImageProcessorImpl(
            context = context,
            ioDispatcher = Dispatchers.Unconfined
        )
    }
    
    @Test
    fun `validateImage should return true for valid JPEG`() = runTest {
        val uri = mockk<Uri>()
        val testImageData = createMockImageData(100, 100)
        
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(testImageData)
        every { contentResolver.getType(uri) } returns "image/jpeg"
        
        val result = imageProcessor.validateImage(uri)
        
        assertTrue(result.isSuccess)
        assertTrue(result.getOrDefault(false))
    }
    
    @Test
    fun `validateImage should return true for valid PNG`() = runTest {
        val uri = mockk<Uri>()
        val testImageData = createMockImageData(200, 200)
        
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(testImageData)
        every { contentResolver.getType(uri) } returns "image/png"
        
        val result = imageProcessor.validateImage(uri)
        
        assertTrue(result.isSuccess)
        assertTrue(result.getOrDefault(false))
    }
    
    @Test
    fun `validateImage should return false for unsupported MIME type`() = runTest {
        val uri = mockk<Uri>()
        val testImageData = ByteArray(1024)
        
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(testImageData)
        every { contentResolver.getType(uri) } returns "video/mp4"
        
        val result = imageProcessor.validateImage(uri)
        
        assertTrue(result.isSuccess)
        assertFalse(result.getOrDefault(true))
    }
    
    @Test
    fun `validateImage should return false for null URI`() = runTest {
        val uri = mockk<Uri>()
        
        every { contentResolver.openInputStream(uri) } returns null
        
        val result = imageProcessor.validateImage(uri)
        
        assertTrue(result.isSuccess)
        assertFalse(result.getOrDefault(true))
    }
    
    @Test
    fun `validateImage should return false for image larger than max size`() = runTest {
        val uri = mockk<Uri>()
        // Create data larger than 10MB
        val largeData = ByteArray(11 * 1024 * 1024)
        
        val inputStream = mockk<InputStream>()
        every { inputStream.available() } returns largeData.size
        every { inputStream.use<InputStream, Any>(any()) } answers {
            val block = firstArg<(InputStream) -> Any>()
            block(inputStream)
        }
        
        every { contentResolver.openInputStream(uri) } returns inputStream
        every { contentResolver.getType(uri) } returns "image/jpeg"
        
        val result = imageProcessor.validateImage(uri)
        
        assertTrue(result.isSuccess)
        assertFalse(result.getOrDefault(true))
    }
    
    @Test
    fun `preprocessImage should handle null bitmap gracefully`() = runTest {
        val uri = mockk<Uri>()
        
        every { contentResolver.openInputStream(uri) } returns null
        
        val result = imageProcessor.preprocessImage(uri, 512, ImageFormat.JPEG)
        
        assertTrue(result.isFailure)
    }
    
    // Helper function to create mock image data
    private fun createMockImageData(width: Int, height: Int): ByteArray {
        // Create a simple bitmap and compress to bytes
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        bitmap.recycle()
        return outputStream.toByteArray()
    }
}
