package com.nervesparks.iris.core.tools.executor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.nervesparks.iris.core.tools.models.ExecutionResult
import com.nervesparks.iris.core.tools.models.ExecutionType
import com.nervesparks.iris.core.tools.models.FunctionCall
import com.nervesparks.iris.core.tools.models.ToolDefinition
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executor for tools that launch Android intents
 */
@Singleton
class IntentLaunchExecutor @Inject constructor() : ToolExecutor {
    
    override suspend fun execute(
        context: Context,
        toolDefinition: ToolDefinition,
        functionCall: FunctionCall
    ): ExecutionResult {
        return try {
            val intent = buildIntent(toolDefinition, functionCall)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            
            ExecutionResult.Success(
                data = "Intent launched successfully",
                message = "Opened ${toolDefinition.name} successfully"
            )
        } catch (e: Exception) {
            ExecutionResult.Error(
                error = "Failed to launch intent: ${e.message}",
                throwable = e
            )
        }
    }
    
    override fun canExecute(toolDefinition: ToolDefinition): Boolean {
        return toolDefinition.executionType == ExecutionType.INTENT_LAUNCH
    }
    
    /**
     * Build appropriate intent based on tool name
     */
    private fun buildIntent(toolDefinition: ToolDefinition, functionCall: FunctionCall): Intent {
        return when (toolDefinition.name) {
            "create_calendar_event" -> buildCalendarIntent(functionCall)
            "send_sms" -> buildSmsIntent(functionCall)
            "set_alarm" -> buildAlarmIntent(functionCall)
            "set_timer" -> buildTimerIntent(functionCall)
            "web_search" -> buildWebSearchIntent(functionCall)
            else -> throw IllegalArgumentException("Unknown tool: ${toolDefinition.name}")
        }
    }
    
    private fun buildCalendarIntent(functionCall: FunctionCall): Intent {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, functionCall.arguments["title"])
            
            // Parse datetime
            functionCall.arguments["datetime"]?.let { datetime ->
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    val date = dateFormat.parse(datetime)
                    date?.let {
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it.time)
                        
                        // Add duration
                        val durationMins = functionCall.arguments["duration_mins"]?.toIntOrNull() ?: 60
                        val endTime = it.time + (durationMins * 60 * 1000)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                    }
                } catch (e: Exception) {
                    // Skip invalid datetime
                }
            }
            
            functionCall.arguments["location"]?.let {
                putExtra(CalendarContract.Events.EVENT_LOCATION, it)
            }
            
            functionCall.arguments["description"]?.let {
                putExtra(CalendarContract.Events.DESCRIPTION, it)
            }
        }
        return intent
    }
    
    private fun buildSmsIntent(functionCall: FunctionCall): Intent {
        val phoneNumber = functionCall.arguments["to"] ?: ""
        val message = functionCall.arguments["message"] ?: ""
        
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
        }
        return intent
    }
    
    private fun buildAlarmIntent(functionCall: FunctionCall): Intent {
        val hour = functionCall.arguments["hour"]?.toIntOrNull() ?: 0
        val minute = functionCall.arguments["minute"]?.toIntOrNull() ?: 0
        
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            
            functionCall.arguments["message"]?.let {
                putExtra(AlarmClock.EXTRA_MESSAGE, it)
            }
            
            // Parse days if provided
            functionCall.arguments["days"]?.let { daysStr ->
                val days = parseDays(daysStr)
                if (days.isNotEmpty()) {
                    putExtra(AlarmClock.EXTRA_DAYS, ArrayList(days))
                }
            }
        }
        return intent
    }
    
    private fun buildTimerIntent(functionCall: FunctionCall): Intent {
        val seconds = functionCall.arguments["seconds"]?.toIntOrNull() ?: 60
        
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            
            functionCall.arguments["message"]?.let {
                putExtra(AlarmClock.EXTRA_MESSAGE, it)
            }
        }
        return intent
    }
    
    private fun buildWebSearchIntent(functionCall: FunctionCall): Intent {
        val query = functionCall.arguments["query"] ?: ""
        val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(searchUrl)
        }
        return intent
    }
    
    /**
     * Parse day abbreviations to Calendar day constants
     */
    private fun parseDays(daysStr: String): List<Int> {
        val dayMap = mapOf(
            "MON" to 2, "TUE" to 3, "WED" to 4, "THU" to 5,
            "FRI" to 6, "SAT" to 7, "SUN" to 1
        )
        
        return daysStr.split(",")
            .map { it.trim().uppercase() }
            .mapNotNull { dayMap[it] }
    }
}
