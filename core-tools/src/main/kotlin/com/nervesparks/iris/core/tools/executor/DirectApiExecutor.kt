package com.nervesparks.iris.core.tools.executor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.nervesparks.iris.core.tools.models.ExecutionResult
import com.nervesparks.iris.core.tools.models.ExecutionType
import com.nervesparks.iris.core.tools.models.FunctionCall
import com.nervesparks.iris.core.tools.models.ToolDefinition
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executor for tools that use direct Android APIs
 */
@Singleton
class DirectApiExecutor @Inject constructor() : ToolExecutor {
    
    override suspend fun execute(
        context: Context,
        toolDefinition: ToolDefinition,
        functionCall: FunctionCall
    ): ExecutionResult {
        return when (toolDefinition.name) {
            "search_contacts" -> searchContacts(context, functionCall)
            else -> ExecutionResult.Error("Unknown direct API tool: ${toolDefinition.name}")
        }
    }
    
    override fun canExecute(toolDefinition: ToolDefinition): Boolean {
        return toolDefinition.executionType == ExecutionType.DIRECT_API
    }
    
    /**
     * Search contacts by name
     */
    private fun searchContacts(context: Context, functionCall: FunctionCall): ExecutionResult {
        // Check permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return ExecutionResult.PermissionDenied(
                listOf(Manifest.permission.READ_CONTACTS)
            )
        }
        
        val query = functionCall.arguments["query"] ?: return ExecutionResult.Error("Missing query parameter")
        
        return try {
            val contacts = mutableListOf<String>()
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
                ),
                "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                ContactsContract.Contacts.DISPLAY_NAME
            )
            
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                while (it.moveToNext() && contacts.size < 10) { // Limit to 10 results
                    val name = it.getString(nameIndex)
                    contacts.add(name)
                }
            }
            
            if (contacts.isEmpty()) {
                ExecutionResult.Success(
                    data = "[]",
                    message = "No contacts found matching '$query'"
                )
            } else {
                ExecutionResult.Success(
                    data = contacts.joinToString(", "),
                    message = "Found ${contacts.size} contact(s): ${contacts.joinToString(", ")}"
                )
            }
        } catch (e: Exception) {
            ExecutionResult.Error(
                error = "Failed to search contacts: ${e.message}",
                throwable = e
            )
        }
    }
}
