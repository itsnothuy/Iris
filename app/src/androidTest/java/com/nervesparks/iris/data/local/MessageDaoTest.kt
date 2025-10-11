package com.nervesparks.iris.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented tests for MessageDao.
 * 
 * These tests run on an Android device and use an in-memory database.
 */
@RunWith(AndroidJUnit4::class)
class MessageDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var messageDao: MessageDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        messageDao = database.messageDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertMessage_andRetrieve() = runBlocking {
        val message = MessageEntity(
            id = "test-1",
            content = "Test message",
            role = "USER",
            timestamp = System.currentTimeMillis(),
            processingTimeMs = null,
            tokenCount = null
        )
        
        messageDao.insertMessage(message)
        val messages = messageDao.getAllMessagesList()
        
        assertEquals(1, messages.size)
        assertEquals(message.id, messages[0].id)
        assertEquals(message.content, messages[0].content)
        assertEquals(message.role, messages[0].role)
    }
    
    @Test
    fun insertMultipleMessages_retrievedInOrder() = runBlocking {
        val message1 = MessageEntity("1", "First", "USER", 1000L, null, null)
        val message2 = MessageEntity("2", "Second", "ASSISTANT", 2000L, null, null)
        val message3 = MessageEntity("3", "Third", "USER", 3000L, null, null)
        
        messageDao.insertMessage(message1)
        messageDao.insertMessage(message2)
        messageDao.insertMessage(message3)
        
        val messages = messageDao.getAllMessagesList()
        
        assertEquals(3, messages.size)
        assertEquals("First", messages[0].content)
        assertEquals("Second", messages[1].content)
        assertEquals("Third", messages[2].content)
    }
    
    @Test
    fun insertMessages_batchInsert() = runBlocking {
        val messages = listOf(
            MessageEntity("1", "Message 1", "USER", 1000L, null, null),
            MessageEntity("2", "Message 2", "ASSISTANT", 2000L, 1500L, 50),
            MessageEntity("3", "Message 3", "USER", 3000L, null, null)
        )
        
        messageDao.insertMessages(messages)
        val retrieved = messageDao.getAllMessagesList()
        
        assertEquals(3, retrieved.size)
        assertEquals(1500L, retrieved[1].processingTimeMs)
        assertEquals(50, retrieved[1].tokenCount)
    }
    
    @Test
    fun deleteMessage_removesSpecificMessage() = runBlocking {
        val message1 = MessageEntity("1", "First", "USER", 1000L, null, null)
        val message2 = MessageEntity("2", "Second", "ASSISTANT", 2000L, null, null)
        
        messageDao.insertMessage(message1)
        messageDao.insertMessage(message2)
        messageDao.deleteMessage("1")
        
        val messages = messageDao.getAllMessagesList()
        
        assertEquals(1, messages.size)
        assertEquals("Second", messages[0].content)
    }
    
    @Test
    fun deleteAllMessages_clearsDatabase() = runBlocking {
        val messages = listOf(
            MessageEntity("1", "Message 1", "USER", 1000L, null, null),
            MessageEntity("2", "Message 2", "ASSISTANT", 2000L, null, null),
            MessageEntity("3", "Message 3", "USER", 3000L, null, null)
        )
        
        messageDao.insertMessages(messages)
        messageDao.deleteAllMessages()
        
        val retrieved = messageDao.getAllMessagesList()
        assertEquals(0, retrieved.size)
    }
    
    @Test
    fun getMessageCount_returnsCorrectCount() = runBlocking {
        assertEquals(0, messageDao.getMessageCount())
        
        messageDao.insertMessage(MessageEntity("1", "First", "USER", 1000L, null, null))
        assertEquals(1, messageDao.getMessageCount())
        
        messageDao.insertMessage(MessageEntity("2", "Second", "ASSISTANT", 2000L, null, null))
        assertEquals(2, messageDao.getMessageCount())
    }
    
    @Test
    fun getAllMessages_returnsFlowOfMessages() = runBlocking {
        val message = MessageEntity("1", "Test", "USER", 1000L, null, null)
        messageDao.insertMessage(message)
        
        val messages = messageDao.getAllMessages().first()
        
        assertEquals(1, messages.size)
        assertEquals("Test", messages[0].content)
    }
    
    @Test
    fun insertMessage_withConflict_replaces() = runBlocking {
        val message1 = MessageEntity("same-id", "Original", "USER", 1000L, null, null)
        val message2 = MessageEntity("same-id", "Updated", "ASSISTANT", 2000L, null, null)
        
        messageDao.insertMessage(message1)
        messageDao.insertMessage(message2)
        
        val messages = messageDao.getAllMessagesList()
        
        assertEquals(1, messages.size)
        assertEquals("Updated", messages[0].content)
        assertEquals("ASSISTANT", messages[0].role)
    }
    
    @Test
    fun emptyDatabase_returnsEmptyList() = runBlocking {
        val messages = messageDao.getAllMessagesList()
        assertTrue(messages.isEmpty())
    }
    
    @Test
    fun insertMessage_withProcessingMetrics() = runBlocking {
        val message = MessageEntity(
            id = "test-metrics",
            content = "Test",
            role = "ASSISTANT",
            timestamp = System.currentTimeMillis(),
            processingTimeMs = 2500L,
            tokenCount = 150
        )
        
        messageDao.insertMessage(message)
        val retrieved = messageDao.getAllMessagesList()
        
        assertEquals(2500L, retrieved[0].processingTimeMs)
        assertEquals(150, retrieved[0].tokenCount)
    }
}
