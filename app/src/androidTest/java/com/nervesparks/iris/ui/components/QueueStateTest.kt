package com.nervesparks.iris.ui.components

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.ui.theme.IrisTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for queue state affordances.
 * Tests verify that the UI correctly displays queue status to the user.
 */
@RunWith(AndroidJUnit4::class)
class QueueStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun queueState_notQueued_doesNotShowQueueIndicator() {
        composeTestRule.setContent {
            IrisTheme {
                Surface {
                    QueueStateIndicator(
                        isQueued = false,
                        queueSize = 0
                    )
                }
            }
        }

        // Verify queue indicator is not shown
        composeTestRule.onNodeWithText("Queued").assertDoesNotExist()
        composeTestRule.onNodeWithTag("queue-indicator").assertDoesNotExist()
    }

    @Test
    fun queueState_queued_showsQueueIndicator() {
        composeTestRule.setContent {
            IrisTheme {
                Surface {
                    QueueStateIndicator(
                        isQueued = true,
                        queueSize = 1
                    )
                }
            }
        }

        // Verify queue indicator is shown
        composeTestRule.onNodeWithTag("queue-indicator").assertExists()
        composeTestRule.onNodeWithTag("queue-indicator").assertIsDisplayed()
    }

    @Test
    fun queueState_showsCorrectQueueSize() {
        val testCases = listOf(1, 2, 3)
        
        testCases.forEach { size ->
            composeTestRule.setContent {
                IrisTheme {
                    Surface {
                        QueueStateIndicator(
                            isQueued = true,
                            queueSize = size
                        )
                    }
                }
            }

            // Verify queue size is displayed correctly
            composeTestRule.onNodeWithText("$size in queue", substring = true).assertExists()
        }
    }

    @Test
    fun queueState_singularForm_forSingleMessage() {
        composeTestRule.setContent {
            IrisTheme {
                Surface {
                    QueueStateIndicator(
                        isQueued = true,
                        queueSize = 1
                    )
                }
            }
        }

        // Verify singular form is used
        composeTestRule.onNodeWithText("1 message in queue", substring = true).assertExists()
    }

    @Test
    fun queueState_pluralForm_forMultipleMessages() {
        composeTestRule.setContent {
            IrisTheme {
                Surface {
                    QueueStateIndicator(
                        isQueued = true,
                        queueSize = 2
                    )
                }
            }
        }

        // Verify plural form is used
        composeTestRule.onNodeWithText("2 messages in queue", substring = true).assertExists()
    }

    @Test
    fun queueState_transitionsFromNotQueuedToQueued() {
        var isQueued = false
        var queueSize = 0
        
        composeTestRule.setContent {
            IrisTheme {
                Surface {
                    QueueStateIndicator(
                        isQueued = isQueued,
                        queueSize = queueSize
                    )
                }
            }
        }

        // Initially not queued
        composeTestRule.onNodeWithTag("queue-indicator").assertDoesNotExist()
        
        // Transition to queued
        isQueued = true
        queueSize = 1
        composeTestRule.setContent {
            IrisTheme {
                Surface {
                    QueueStateIndicator(
                        isQueued = isQueued,
                        queueSize = queueSize
                    )
                }
            }
        }
        
        // Now shows queue indicator
        composeTestRule.onNodeWithTag("queue-indicator").assertExists()
    }

    @Test
    fun queueState_isVisuallyDistinct() {
        composeTestRule.setContent {
            IrisTheme {
                Surface {
                    QueueStateIndicator(
                        isQueued = true,
                        queueSize = 2
                    )
                }
            }
        }

        // Verify the indicator is actually displayed on screen
        composeTestRule.onNodeWithTag("queue-indicator")
            .assertIsDisplayed()
    }
}
