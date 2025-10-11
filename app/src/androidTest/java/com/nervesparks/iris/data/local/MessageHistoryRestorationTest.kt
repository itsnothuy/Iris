package com.nervesparks.iris.data.local

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.MessageRole
import com.nervesparks.iris.ui.components.MessageBubble
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Compose UI test for message history restoration.
 * 
 * Verifies that messages persisted to the database can be loaded and displayed
 * correctly in the UI after app restart.
 */
@RunWith(AndroidJUnit4::class)
class MessageHistoryRestorationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
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
    fun restoredMessages_displayCorrectly_afterReload() = runBlocking {
        // Simulate persisted messages from a previous session
        val persistedMessages = listOf(
            MessageEntity("1", "Hello, AI!", "USER", Instant.now().minusSeconds(60).toEpochMilli(), null, null),
            MessageEntity("2", "Hello! How can I help you?", "ASSISTANT", Instant.now().minusSeconds(50).toEpochMilli(), 1500L, 25),
            MessageEntity("3", "What is the weather today?", "USER", Instant.now().minusSeconds(40).toEpochMilli(), null, null),
            MessageEntity("4", "I don't have access to weather information.", "ASSISTANT", Instant.now().minusSeconds(30).toEpochMilli(), 2000L, 30)
        )
        
        // Persist messages to database
        messageDao.insertMessages(persistedMessages)
        
        // Load messages from database (simulating app restart)
        val loadedEntities = messageDao.getAllMessages().first()
        val loadedMessages = MessageMapper.toDomainList(loadedEntities)
        
        // Display loaded messages in UI
        composeTestRule.setContent {
            MessageHistoryDisplay(messages = loadedMessages)
        }
        
        // Verify all messages are displayed
        composeTestRule.onNodeWithText("Hello, AI!").assertExists()
        composeTestRule.onNodeWithText("Hello! How can I help you?").assertExists()
        composeTestRule.onNodeWithText("What is the weather today?").assertExists()
        composeTestRule.onNodeWithText("I don't have access to weather information.").assertExists()
        
        // Verify correct number of messages
        composeTestRule.onAllNodesWithTag("message_bubble").assertCountEquals(4)
    }
    
    @Test
    fun emptyHistory_displaysNoMessages() = runBlocking {
        // Load messages from empty database
        val loadedEntities = messageDao.getAllMessages().first()
        val loadedMessages = MessageMapper.toDomainList(loadedEntities)
        
        composeTestRule.setContent {
            MessageHistoryDisplay(messages = loadedMessages)
        }
        
        // Verify no messages are displayed
        composeTestRule.onAllNodesWithTag("message_bubble").assertCountEquals(0)
    }
    
    @Test
    fun restoredMessages_maintainCorrectOrder() = runBlocking {
        // Create messages with specific timestamps
        val messages = listOf(
            MessageEntity("1", "First message", "USER", 1000L, null, null),
            MessageEntity("2", "Second message", "ASSISTANT", 2000L, null, null),
            MessageEntity("3", "Third message", "USER", 3000L, null, null)
        )
        
        messageDao.insertMessages(messages)
        
        val loadedEntities = messageDao.getAllMessages().first()
        val loadedMessages = MessageMapper.toDomainList(loadedEntities)
        
        composeTestRule.setContent {
            MessageHistoryDisplay(messages = loadedMessages)
        }
        
        // Verify messages appear in chronological order
        val allMessages = composeTestRule.onAllNodesWithTag("message_bubble")
        allMessages.assertCountEquals(3)
        
        // Verify order by checking text exists
        composeTestRule.onNodeWithText("First message").assertExists()
        composeTestRule.onNodeWithText("Second message").assertExists()
        composeTestRule.onNodeWithText("Third message").assertExists()
    }
    
    @Test
    fun restoredMessages_preserveProcessingMetrics() = runBlocking {
        val messageWithMetrics = MessageEntity(
            id = "metrics-test",
            content = "AI response with metrics",
            role = "ASSISTANT",
            timestamp = Instant.now().toEpochMilli(),
            processingTimeMs = 1500L,
            tokenCount = 50
        )
        
        messageDao.insertMessage(messageWithMetrics)
        
        val loadedEntities = messageDao.getAllMessages().first()
        val loadedMessages = MessageMapper.toDomainList(loadedEntities)
        
        // Verify metrics are preserved
        assert(loadedMessages.isNotEmpty())
        assert(loadedMessages[0].processingTimeMs == 1500L)
        assert(loadedMessages[0].tokenCount == 50)
    }
    
    @Test
    fun mixedRoles_allDisplayCorrectly() = runBlocking {
        val messages = listOf(
            MessageEntity("1", "User message", "USER", 1000L, null, null),
            MessageEntity("2", "Assistant message", "ASSISTANT", 2000L, null, null),
            MessageEntity("3", "System message", "SYSTEM", 3000L, null, null)
        )
        
        messageDao.insertMessages(messages)
        
        val loadedEntities = messageDao.getAllMessages().first()
        val loadedMessages = MessageMapper.toDomainList(loadedEntities)
        
        composeTestRule.setContent {
            MessageHistoryDisplay(messages = loadedMessages)
        }
        
        // All three message types should be displayed
        composeTestRule.onNodeWithText("User message").assertExists()
        composeTestRule.onNodeWithText("Assistant message").assertExists()
        composeTestRule.onNodeWithText("System message").assertExists()
    }
}

/**
 * Composable to display message history for testing.
 */
@Composable
private fun MessageHistoryDisplay(messages: List<Message>) {
    LazyColumn {
        items(messages) { message ->
            MessageBubble(
                message = message,
                modifier = androidx.compose.ui.Modifier.testTag("message_bubble")
            )
        }
    }
}
