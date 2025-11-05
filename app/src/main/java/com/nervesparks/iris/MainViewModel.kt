package com.nervesparks.iris

import android.content.Context
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.annotation.VisibleForTesting
import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.MessageRole
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import java.io.File
import java.time.Instant
import java.util.Locale
import java.util.UUID

class MainViewModel(
    private val llamaAndroid: LLamaAndroid = LLamaAndroid.instance(), 
    private val userPreferencesRepository: UserPreferencesRepository,
    private val messageRepository: MessageRepository? = null,
    private val conversationRepository: com.nervesparks.iris.data.repository.ConversationRepository? = null
): ViewModel() {
    companion object {
//        @JvmStatic
//        private val NanosPerSecond = 1_000_000_000.0
    }


    private val _defaultModelName = mutableStateOf("")
    val defaultModelName: State<String> = _defaultModelName
    
    // Current conversation ID - defaults to "default" for backward compatibility
    var currentConversationId by mutableStateOf("default")
        private set

    init {
        loadDefaultModelName()
        ensureDefaultConversationExists()
        restoreMessagesFromDatabase()
    }
    
    private fun loadDefaultModelName(){
        _defaultModelName.value = userPreferencesRepository.getDefaultModelName()
    }
    
    /**
     * Ensure the default conversation exists in the database.
     * This provides backward compatibility for existing installations.
     */
    private fun ensureDefaultConversationExists() {
        conversationRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    val existing = repo.getConversationById("default")
                    if (existing == null) {
                        // Create default conversation if it doesn't exist
                        val defaultConversation = com.nervesparks.iris.data.Conversation(
                            id = "default",
                            title = "Conversation",
                            createdAt = Instant.now(),
                            lastModified = Instant.now()
                        )
                        repo.createConversation(defaultConversation)
                        Log.i(tag, "Created default conversation")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to ensure default conversation exists", e)
                }
            }
        }
    }
    
    /**
     * Restore messages from database at startup.
     */
    private fun restoreMessagesFromDatabase() {
        messageRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    val savedMessages = repo.getMessagesForConversationList(currentConversationId)
                    if (savedMessages.isNotEmpty()) {
                        // Convert domain Messages back to Map format for compatibility
                        messages = savedMessages.map { msg ->
                            mapOf(
                                "role" to msg.role.name.lowercase(),
                                "content" to msg.content
                            )
                        }
                        first = false // Already have conversation history
                        Log.i(tag, "Restored ${savedMessages.size} messages from database")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to restore messages from database", e)
                }
            }
        }
    }
    
    /**
     * Switch to a different conversation.
     * Loads messages for the specified conversation.
     */
    fun switchConversation(conversationId: String) {
        currentConversationId = conversationId
        messageRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    val savedMessages = repo.getMessagesForConversationList(conversationId)
                    messages = savedMessages.map { msg ->
                        mapOf(
                            "role" to msg.role.name.lowercase(),
                            "content" to msg.content
                        )
                    }
                    first = messages.isEmpty()
                    Log.i(tag, "Switched to conversation $conversationId with ${savedMessages.size} messages")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to switch conversation", e)
                }
            }
        }
    }
    
    /**
     * Create a new conversation and switch to it.
     */
    fun createNewConversation() {
        conversationRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    val newConversation = com.nervesparks.iris.data.Conversation(
                        title = "New Conversation",
                        createdAt = Instant.now(),
                        lastModified = Instant.now()
                    )
                    repo.createConversation(newConversation)
                    switchConversation(newConversation.id)
                    Log.i(tag, "Created new conversation ${newConversation.id}")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to create new conversation", e)
                }
            }
        }
    }
    
    /**
     * Delete a conversation by ID.
     * If deleting the current conversation, switches to default.
     */
    fun deleteConversation(conversationId: String) {
        conversationRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    repo.deleteConversation(conversationId)
                    Log.i(tag, "Deleted conversation $conversationId")
                    
                    // If we deleted the current conversation, switch to default
                    if (conversationId == currentConversationId) {
                        switchConversation("default")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to delete conversation", e)
                }
            }
        }
    }
    
    /**
     * Delete all conversations and messages.
     * This is a destructive operation that cannot be undone.
     */
    fun deleteAllData() {
        conversationRepository?.let { convRepo ->
            messageRepository?.let { msgRepo ->
                viewModelScope.launch {
                    try {
                        // Clear messages
                        messages.clear()
                        
                        // Delete all data from repositories
                        convRepo.deleteAllConversations()
                        msgRepo.deleteAllMessages()
                        
                        // Reset to default state
                        currentConversationId = "default"
                        createDefaultConversation()
                        
                        Log.i(tag, "Deleted all conversations and messages")
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to delete all data", e)
                    }
                }
            }
        }
    }
    
    /**
     * Toggle pin status for a conversation.
     */
    fun toggleConversationPin(conversationId: String, isPinned: Boolean) {
        conversationRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    repo.togglePin(conversationId, isPinned)
                    Log.i(tag, "Toggled pin for conversation $conversationId to $isPinned")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to toggle pin", e)
                }
            }
        }
    }
    
    /**
     * Toggle archive status for a conversation.
     */
    fun toggleConversationArchive(conversationId: String, isArchived: Boolean) {
        conversationRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    repo.toggleArchive(conversationId, isArchived)
                    Log.i(tag, "Toggled archive for conversation $conversationId to $isArchived")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to toggle archive", e)
                }
            }
        }
    }
    
    /**
     * Get all conversations as a Flow.
     */
    fun getAllConversations() = conversationRepository?.getAllConversations()
    
    /**
     * Search conversations by query.
     */
    fun searchConversations(query: String) = conversationRepository?.searchConversations(query)

    fun setDefaultModelName(modelName: String){
        userPreferencesRepository.setDefaultModelName(modelName)
        _defaultModelName.value = modelName
    }

    lateinit var selectedModel: String
    private val tag: String? = this::class.simpleName

    @set:VisibleForTesting
    var messages by mutableStateOf(

            listOf<Map<String, String>>(),
        )
        private set
    var newShowModal by mutableStateOf(false)
    var showDownloadInfoModal by mutableStateOf(false)
    var user_thread by mutableStateOf(0f)
    var topP by mutableStateOf(0f)
    var topK by mutableStateOf(0)
    var temp by mutableStateOf(0f)

    var allModels by mutableStateOf(
        listOf(
            mapOf(
                "name" to "Llama-3.2-1B-Instruct-Q6_K_L.gguf",
                "source" to "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q6_K_L.gguf?download=true",
                "destination" to "Llama-3.2-1B-Instruct-Q6_K_L.gguf"
            ),
            mapOf(
                "name" to "Llama-3.2-3B-Instruct-Q4_K_L.gguf",
                "source" to "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_L.gguf?download=true",
                "destination" to "Llama-3.2-3B-Instruct-Q4_K_L.gguf"
            ),
            mapOf(
                "name" to "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf",
                "source" to "https://huggingface.co/Crataco/stablelm-2-1_6b-chat-imatrix-GGUF/resolve/main/stablelm-2-1_6b-chat.Q4_K_M.imx.gguf?download=true",
                "destination" to "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf"
            ),

        )
    )

    private var first by mutableStateOf(
        true
    )
    var userSpecifiedThreads by mutableIntStateOf(2)
    var message by mutableStateOf("")
        private set

    var userGivenModel by mutableStateOf("")
    var SearchedName by mutableStateOf("")

    private var textToSpeech:TextToSpeech? = null

    var textForTextToSpeech = ""
    var stateForTextToSpeech by mutableStateOf(true)
        private set

    var eot_str = ""


    var refresh by mutableStateOf(false)

    fun loadExistingModels(directory: File) {
        // List models in the directory that end with .gguf
        directory.listFiles { file -> file.extension == "gguf" }?.forEach { file ->
            val modelName = file.name
            Log.i("This is the modelname", modelName)
            if (!allModels.any { it["name"] == modelName }) {
                allModels += mapOf(
                    "name" to modelName,
                    "source" to "local",
                    "destination" to file.name
                )
            }
        }

        if (defaultModelName.value.isNotEmpty()) {
            val loadedDefaultModel = allModels.find { model -> model["name"] == defaultModelName.value }

            if (loadedDefaultModel != null) {
                val destinationPath = File(directory, loadedDefaultModel["destination"].toString())
                if(loadedModelName.value == "") {
                    load(destinationPath.path, userThreads = user_thread.toInt())
                }
                currentDownloadable = Downloadable(
                    loadedDefaultModel["name"].toString(),
                    Uri.parse(loadedDefaultModel["source"].toString()),
                    destinationPath
                )
            } else {
                // Handle case where the model is not found
                allModels.find { model ->
                    val destinationPath = File(directory, model["destination"].toString())
                    destinationPath.exists()
                }?.let { model ->
                    val destinationPath = File(directory, model["destination"].toString())
                    if(loadedModelName.value == "") {
                        load(destinationPath.path, userThreads = user_thread.toInt())
                    }
                    currentDownloadable = Downloadable(
                        model["name"].toString(),
                        Uri.parse(model["source"].toString()),
                        destinationPath
                    )
                }
            }
        } else{
            allModels.find { model ->
                val destinationPath = File(directory, model["destination"].toString())
                destinationPath.exists()
            }?.let { model ->
                val destinationPath = File(directory, model["destination"].toString())
                if(loadedModelName.value == "") {
                    load(destinationPath.path, userThreads = user_thread.toInt())
                }
                currentDownloadable = Downloadable(
                    model["name"].toString(),
                    Uri.parse(model["source"].toString()),
                    destinationPath
                )
            }
        // Attempt to find and load the first model that exists in the combined logic

         }
    }



    fun textToSpeech(context: Context) {
        if (!getIsSending()) {
            // If TTS is already initialized, stop it first
            textToSpeech?.stop()

            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.let { txtToSpeech ->
                        txtToSpeech.language = Locale.US
                        txtToSpeech.setSpeechRate(1.0f)

                        // Add a unique utterance ID for tracking
                        val utteranceId = UUID.randomUUID().toString()

                        txtToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onDone(utteranceId: String?) {
                                // Reset state when speech is complete
                                CoroutineScope(Dispatchers.Main).launch {
                                    stateForTextToSpeech = true
                                }
                            }

                            override fun onError(utteranceId: String?) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    stateForTextToSpeech = true
                                }
                            }

                            override fun onStart(utteranceId: String?) {
                                // Update state to indicate speech is playing
                                CoroutineScope(Dispatchers.Main).launch {
                                    stateForTextToSpeech = false
                                }
                            }
                        })

                        txtToSpeech.speak(
                            textForTextToSpeech,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            utteranceId
                        )
                    }
                }
            }
        }
    }



    fun stopTextToSpeech() {
        textToSpeech?.apply {
            stop()  // Stops current speech
            shutdown()  // Releases the resources
        }
        textToSpeech = null

        // Reset state to allow restarting
        stateForTextToSpeech = true
    }



    var toggler by mutableStateOf(false)
    var showModal by  mutableStateOf(true)
    var showAlert by mutableStateOf(false)
    var switchModal by mutableStateOf(false)
    var currentDownloadable: Downloadable? by mutableStateOf(null)
    
    // Error state for UI
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    // Queue state for UI
    var isMessageQueued by mutableStateOf(false)
        private set
    var queueSize by mutableStateOf(0)
        private set
    
    // Redaction state for UI
    var wasLastMessageRedacted by mutableStateOf(false)
        private set
    var lastRedactionCount by mutableIntStateOf(0)
        private set
    
    // Rate-limit and thermal state for UI
    var isRateLimited by mutableStateOf(false)
        private set
    var isThermalThrottled by mutableStateOf(false)
        private set
    var rateLimitCooldownSeconds by mutableStateOf(0)
        private set

    override fun onCleared() {
        textToSpeech?.shutdown()
        super.onCleared()

        viewModelScope.launch {
            try {

                llamaAndroid.unload()

            } catch (exc: IllegalStateException) {
                addMessage("error", exc.message ?: "")
            }
        }
    }

    fun send() {
        val userMessage = removeExtraWhiteSpaces(message)
        message = ""
        
        // Clear any previous errors when sending a new message
        clearError()
        
        // Reset redaction state
        wasLastMessageRedacted = false
        lastRedactionCount = 0

        // Add to messages console.
        if (userMessage != "" && userMessage != " ") {
            if(first){
                addMessage("system", "This is a conversation between User and Iris, a friendly chatbot. Iris is helpful, kind, honest, good at writing, and never fails to answer any requests immediately and with precision.")
                addMessage("user", "Hi")
                persistInitialMessage("user", "Hi")
                addMessage("assistant", "How may I help You?")
                persistInitialMessage("assistant", "How may I help You?")
                first = false
            }

            // Apply privacy redaction if enabled
            val finalMessage = if (userPreferencesRepository.getPrivacyRedactionEnabled()) {
                val redactionResult = com.nervesparks.iris.util.PrivacyGuard.redactPII(userMessage)
                if (redactionResult.wasRedacted) {
                    wasLastMessageRedacted = true
                    lastRedactionCount = redactionResult.redactionCount
                    Log.i(tag, "Redacted ${redactionResult.redactionCount} PII item(s) from user message")
                }
                redactionResult.redactedText
            } else {
                userMessage
            }

            addMessage("user", finalMessage)


            viewModelScope.launch {
                try {
                    val messageToSend = llamaAndroid.getTemplate(messages)
                    
                    // Try to enqueue the message
                    val canSend = llamaAndroid.tryEnqueue(messageToSend)
                    
                    if (!canSend) {
                        // Queue is full, reject the message
                        setError("Too many requests in queue. Please wait and try again.")
                        Log.w(tag, "Message rejected: queue is full")
                        // Remove the user message we just added since it wasn't queued
                        if (messages.isNotEmpty() && messages.last()["role"] == "user") {
                            messages = messages.dropLast(1)
                        }
                        updateQueueState()
                        return@launch
                    }
                    
                    // Update queue state for UI
                    updateQueueState()
                    
                    // Process the message
                    llamaAndroid.send(messageToSend)
                        .catch {
                            Log.e(tag, "send() failed", it)
                            addMessage("error", it.message ?: "")
                            setError(it.message ?: "Failed to process message")
                        }
                        .collect { response ->
                            // Create a new assistant message with the response
                            if (getIsMarked()) {
                                addMessage("codeBlock", response)

                            } else {
                                addMessage("assistant", response)
                            }
                        }
                }
                catch (e: Exception) {
                    Log.e(tag, "send() failed with exception", e)
                    setError(e.message ?: "An unexpected error occurred")
                }
                finally {
                    if (!getIsCompleteEOT()) {
                        trimEOT()
                    }
                    // Persist the complete assistant message after streaming is done
                    persistLastAssistantMessage()
                    
                    // Update queue state after completion
                    updateQueueState()
                    
                    // Check if there's a pending model switch to execute
                    checkAndExecutePendingModelSwitch()
                }



            }
        }



    }

//    fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1) {
//        viewModelScope.launch {
//            try {
//                val start = System.nanoTime()
//                val warmupResult = llamaAndroid.bench(pp, tg, pl, nr)
//                val end = System.nanoTime()
//
//                messages += warmupResult
//
//                val warmup = (end - start).toDouble() / NanosPerSecond
//                messages += "Warm up time: $warmup seconds, please wait..."
//
//                if (warmup > 5.0) {
//                    messages += "Warm up took too long, aborting benchmark"
//                    return@launch
//                }
//
//                messages += llamaAndroid.bench(512, 128, 1, 3)
//            } catch (exc: IllegalStateException) {
//                Log.e(tag, "bench() failed", exc)
//                messages += exc.message!!
//            }
//        }
//    }

    suspend fun unload(){
        llamaAndroid.unload()
    }

    var tokensList = mutableListOf<String>() // Store emitted tokens
    var benchmarkStartTime: Long = 0L // Track the benchmark start time
    var tokensPerSecondsFinal: Double by mutableStateOf(0.0) // Track tokens per second and trigger UI updates
    var isBenchmarkingComplete by mutableStateOf(false) // Flag to track if benchmarking is complete

    fun myCustomBenchmark() {
        viewModelScope.launch {
            try {
                tokensList.clear() // Reset the token list before benchmarking
                benchmarkStartTime = System.currentTimeMillis() // Record the start time
                isBenchmarkingComplete = false // Reset benchmarking flag

                // Launch a coroutine to update the tokens per second every second
                launch {
                    while (!isBenchmarkingComplete) {
                        delay(1000L) // Delay 1 second
                        val elapsedTime = System.currentTimeMillis() - benchmarkStartTime
                        if (elapsedTime > 0) {
                            tokensPerSecondsFinal = tokensList.size.toDouble() / (elapsedTime / 1000.0)
                        }
                    }
                }

                llamaAndroid.myCustomBenchmark()
                    .collect { emittedString ->
                        if (emittedString != null) {
                            tokensList.add(emittedString) // Add each token to the list
                            Log.d(tag, "Token collected: $emittedString")
                        }
                    }
            } catch (exc: IllegalStateException) {
                Log.e(tag, "myCustomBenchmark() failed", exc)
            } catch (exc: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(tag, "myCustomBenchmark() timed out", exc)
            } catch (exc: Exception) {
                Log.e(tag, "Unexpected error during myCustomBenchmark()", exc)
            } finally {
                // Benchmark complete, log the final tokens per second value
                val elapsedTime = System.currentTimeMillis() - benchmarkStartTime
                val finalTokensPerSecond = if (elapsedTime > 0) {
                    tokensList.size.toDouble() / (elapsedTime / 1000.0)
                } else {
                    0.0
                }
                Log.d(tag, "Benchmark complete. Tokens/sec: $finalTokensPerSecond")

                // Update the final tokens per second and stop updating the value
                tokensPerSecondsFinal = finalTokensPerSecond
                isBenchmarkingComplete = true // Mark benchmarking as complete
            }
        }
    }





    var loadedModelName = mutableStateOf("");
    
    // State for model switching
    private var _isSwitchingModel = mutableStateOf(false)
    val isSwitchingModel: State<Boolean> = _isSwitchingModel
    
    private var _pendingModelSwitch: Pair<String, Int>? = null

    fun load(pathToModel: String, userThreads: Int)  {
        viewModelScope.launch {
            try{
                llamaAndroid.unload()
            } catch (exc: IllegalStateException){
                Log.e(tag, "load() failed", exc)
            }
            try {
                var modelName = pathToModel.split("/")
                loadedModelName.value = modelName.last()
                newShowModal = false
                showModal= false
                showAlert = true
                
                // Get parameters from preferences
                val temperature = getTemperature()
                val topP = getTopP()
                val topK = getTopK()
                
                llamaAndroid.load(pathToModel, userThreads = userThreads, topK = topK, topP = topP, temp = temperature)
                showAlert = false

            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
//                addMessage("error", exc.message ?: "")
            }
            showModal = false
            showAlert = false
            eot_str = llamaAndroid.send_eot_str()
        }
    }
    
    /**
     * Request a model switch. If a request is in-flight, the switch will be deferred
     * until the current request completes.
     */
    fun switchModel(pathToModel: String, userThreads: Int) {
        if (getIsSending()) {
            // Defer the switch until the current request completes
            _pendingModelSwitch = Pair(pathToModel, userThreads)
            _isSwitchingModel.value = true
            Log.i(tag, "Model switch deferred until current request completes")
        } else {
            // No request in-flight, switch immediately
            _isSwitchingModel.value = true
            performModelSwitch(pathToModel, userThreads)
        }
    }
    
    /**
     * Perform the actual model switch operation.
     */
    private fun performModelSwitch(pathToModel: String, userThreads: Int) {
        viewModelScope.launch {
            try {
                Log.i(tag, "Starting model switch to: $pathToModel")
                load(pathToModel, userThreads)
                // Update default model preference
                val modelName = pathToModel.split("/").last()
                setDefaultModelName(modelName)
                Log.i(tag, "Model switch completed successfully")
            } catch (e: Exception) {
                Log.e(tag, "Model switch failed", e)
                setError("Failed to switch model: ${e.message}")
            } finally {
                _isSwitchingModel.value = false
                _pendingModelSwitch = null
            }
        }
    }
    
    /**
     * Check if there's a pending model switch and execute it.
     * Should be called after completing a send operation.
     */
    private fun checkAndExecutePendingModelSwitch() {
        _pendingModelSwitch?.let { (path, threads) ->
            Log.i(tag, "Executing pending model switch")
            performModelSwitch(path, threads)
        }
    }
    private fun addMessage(role: String, content: String) {
        val newMessage = mapOf("role" to role, "content" to content)
        val isNewMessage = messages.isEmpty() || messages.last()["role"] != role

        messages = if (!isNewMessage) {
            val lastMessageContent = messages.last()["content"] ?: ""
            val updatedContent = "$lastMessageContent$content"
            val updatedLastMessage = messages.last() + ("content" to updatedContent)
            messages.toMutableList().apply {
                set(messages.lastIndex, updatedLastMessage)
            }
        } else {
            messages + listOf(newMessage)
        }
        
        // Persist complete messages to database (skip system, error, codeBlock, and log messages)
        // For user messages, persist immediately. For assistant messages, we'll persist on completion
        // via a dedicated method since they stream in chunks.
        if (role == "user" && messageRepository != null) {
            viewModelScope.launch {
                try {
                    val domainMessage = Message(
                        content = content,
                        role = MessageRole.USER,
                        timestamp = Instant.now()
                    )
                    messageRepository.saveMessage(domainMessage, currentConversationId)
                    
                    // Update conversation metadata
                    updateConversationMetadata()
                } catch (e: Exception) {
                    Log.e(tag, "Failed to persist message to database", e)
                }
            }
        }
    }
    
    /**
     * Persist the last assistant message to database.
     * Should be called after streaming is complete.
     */
    private fun persistLastAssistantMessage() {
        if (messages.isEmpty() || messageRepository == null) return
        
        val lastMessage = messages.last()
        if (lastMessage["role"] == "assistant") {
            viewModelScope.launch {
                try {
                    val content = lastMessage["content"] ?: ""
                    val domainMessage = Message(
                        content = content,
                        role = MessageRole.ASSISTANT,
                        timestamp = Instant.now()
                    )
                    messageRepository.saveMessage(domainMessage, currentConversationId)
                    Log.i(tag, "Persisted assistant message to database")
                    
                    // Update conversation metadata
                    updateConversationMetadata()
                } catch (e: Exception) {
                    Log.e(tag, "Failed to persist assistant message to database", e)
                }
            }
        }
    }
    
    /**
     * Persist initial conversation messages (for the auto-generated greeting).
     */
    private fun persistInitialMessage(role: String, content: String) {
        if (messageRepository == null) return
        
        viewModelScope.launch {
            try {
                val messageRole = when (role) {
                    "user" -> MessageRole.USER
                    "assistant" -> MessageRole.ASSISTANT
                    else -> return@launch
                }
                val domainMessage = Message(
                    content = content,
                    role = messageRole,
                    timestamp = Instant.now()
                )
                messageRepository.saveMessage(domainMessage, currentConversationId)
                
                // Update conversation metadata
                updateConversationMetadata()
            } catch (e: Exception) {
                Log.e(tag, "Failed to persist initial message to database", e)
            }
        }
    }
    
    /**
     * Update conversation metadata (message count and last modified time).
     */
    private fun updateConversationMetadata() {
        conversationRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    val messageCount = messageRepository?.getMessageCountForConversation(currentConversationId) ?: 0
                    repo.updateConversationMetadata(currentConversationId, messageCount)
                } catch (e: Exception) {
                    Log.e(tag, "Failed to update conversation metadata", e)
                }
            }
        }
    }
    
    /**
     * Delete a message at the specified index.
     * @param messageIndex The index of the message in the messages list (0-based)
     */
    fun deleteMessage(messageIndex: Int) {
        if (messageIndex < 0 || messageIndex >= messages.size) {
            Log.e(tag, "Invalid message index: $messageIndex")
            return
        }
        
        val messageToDelete = messages[messageIndex]
        
        // Remove from in-memory list
        messages = messages.toMutableList().apply {
            removeAt(messageIndex)
        }
        
        // Remove from database if messageRepository is available
        // Note: Since messages in ViewModel are Map<String,String> without IDs,
        // we'll need to delete by content matching or regenerate all messages in DB
        // For now, we'll delete all and re-save to maintain consistency
        messageRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    // Clear and re-save all messages to maintain consistency
                    repo.deleteMessagesForConversation(currentConversationId)
                    messages.forEach { msg ->
                        val role = msg["role"] ?: return@forEach
                        val content = msg["content"] ?: return@forEach
                        if (role in listOf("user", "assistant")) {
                            val messageRole = if (role == "user") MessageRole.USER else MessageRole.ASSISTANT
                            val domainMessage = Message(
                                content = content,
                                role = messageRole,
                                timestamp = Instant.now()
                            )
                            repo.saveMessage(domainMessage, currentConversationId)
                        }
                    }
                    Log.i(tag, "Deleted message at index $messageIndex")
                    
                    // Update conversation metadata
                    updateConversationMetadata()
                } catch (e: Exception) {
                    Log.e(tag, "Failed to delete message from database", e)
                }
            }
        }
    }

    private fun trimEOT() {
        if (messages.isEmpty()) return
        val lastMessageContent = messages.last()["content"] ?: ""
        // Only slice if the content is longer than the EOT string
        if (lastMessageContent.length < eot_str.length) return

        val updatedContent = lastMessageContent.slice(0..(lastMessageContent.length-eot_str.length))
        val updatedLastMessage = messages.last() + ("content" to updatedContent)
        messages = messages.toMutableList().apply {
            set(messages.lastIndex, updatedLastMessage)
        }
        messages.last()["content"]?.let { Log.e(tag, it) }
    }

    private fun removeExtraWhiteSpaces(input: String): String {
        // Replace multiple white spaces with a single space
        return input.replace("\\s+".toRegex(), " ")
    }

    private fun parseTemplateJson(chatData: List<Map<String, String>> ):String{
        var chatStr = ""
        for (data in chatData){
            val role = data["role"]
            val content = data["content"]
            if (role != "log"){
                chatStr += "$role \n$content \n"
            }

        }
        return chatStr
    }
    fun updateMessage(newMessage: String) {
        message = newMessage
    }

    fun clear() {
        messages = listOf(

        )
        first = true
        
        // Clear database for current conversation
        messageRepository?.let { repo ->
            viewModelScope.launch {
                try {
                    repo.deleteMessagesForConversation(currentConversationId)
                    Log.i(tag, "Cleared all messages from conversation $currentConversationId")
                    
                    // Update conversation metadata
                    updateConversationMetadata()
                } catch (e: Exception) {
                    Log.e(tag, "Failed to clear messages from database", e)
                }
            }
        }
    }
    
    /**
     * Get the last user message from the conversation.
     */
    private fun getLastUserMessage(): String? {
        return messages.lastOrNull { it["role"] == "user" }?.get("content")
    }
    
    /**
     * Retry the last user message by resending it verbatim.
     * Removes the last assistant response if it exists before retrying.
     */
    fun retryLastMessage() {
        val lastUserMessage = getLastUserMessage() ?: return
        
        // Remove the last assistant response if it exists
        if (messages.isNotEmpty() && messages.last()["role"] == "assistant") {
            messages = messages.dropLast(1)
        }
        
        // Resend the last user message
        message = lastUserMessage
        send()
    }
    
    /**
     * Edit and resend a user message.
     * Removes the last user message and its corresponding assistant response (if any),
     * then sends the edited message.
     */
    fun editAndResend(editedMessage: String) {
        if (editedMessage.isBlank()) return
        
        // Remove the last assistant response if it exists
        if (messages.isNotEmpty() && messages.last()["role"] == "assistant") {
            messages = messages.dropLast(1)
        }
        
        // Remove the last user message
        if (messages.isNotEmpty() && messages.last()["role"] == "user") {
            messages = messages.dropLast(1)
        }
        
        // Send the edited message
        message = editedMessage
        send()
    }

    fun log(message: String) {
//        addMessage("log", message)
    }

    fun getIsSending(): Boolean {
        return llamaAndroid.getIsSending()
    }

    private fun getIsMarked(): Boolean {
        return llamaAndroid.getIsMarked()
    }

    fun getIsCompleteEOT(): Boolean{
        return llamaAndroid.getIsCompleteEOT()
    }

    fun stop() {
        llamaAndroid.stopTextGeneration()
    }
    
    /**
     * Set an error message to be displayed in the UI.
     */
    fun setError(error: String) {
        errorMessage = error
    }
    
    /**
     * Clear the current error message.
     */
    fun clearError() {
        errorMessage = null
    }
    
    /**
     * Update the queue state from LLamaAndroid.
     */
    private fun updateQueueState() {
        isMessageQueued = llamaAndroid.isQueued()
        queueSize = llamaAndroid.getQueueSize()
        isRateLimited = llamaAndroid.isRateLimited()
        isThermalThrottled = llamaAndroid.isThermalThrottled()
        rateLimitCooldownSeconds = llamaAndroid.getRateLimitCooldownSeconds()
    }
    
    /**
     * Get the privacy redaction enabled status.
     */
    fun getPrivacyRedactionEnabled(): Boolean {
        return userPreferencesRepository.getPrivacyRedactionEnabled()
    }
    
    /**
     * Set the privacy redaction enabled status.
     */
    fun setPrivacyRedactionEnabled(enabled: Boolean) {
        userPreferencesRepository.setPrivacyRedactionEnabled(enabled)
    }
    
    /**
     * Get the theme preference.
     */
    fun getThemePreference(): com.nervesparks.iris.data.ThemePreference {
        return userPreferencesRepository.getThemePreference()
    }
    
    /**
     * Set the theme preference.
     */
    fun setThemePreference(theme: com.nervesparks.iris.data.ThemePreference) {
        userPreferencesRepository.setThemePreference(theme)
    }
    
    /**
     * Get the language preference.
     */
    fun getLanguagePreference(): com.nervesparks.iris.data.LanguagePreference {
        return userPreferencesRepository.getLanguagePreference()
    }
    
    /**
     * Set the language preference.
     */
    fun setLanguagePreference(language: com.nervesparks.iris.data.LanguagePreference) {
        userPreferencesRepository.setLanguagePreference(language)
    }
    
    /**
     * Clear the redaction banner state.
     */
    fun clearRedactionBanner() {
        wasLastMessageRedacted = false
        lastRedactionCount = 0
    }
    
    // Model parameter management
    
    /**
     * Get the current temperature parameter.
     */
    fun getTemperature(): Float {
        return userPreferencesRepository.getTemperature()
    }
    
    /**
     * Set the temperature parameter.
     */
    fun setTemperature(temperature: Float) {
        userPreferencesRepository.setTemperature(temperature)
    }
    
    /**
     * Get the current top_p parameter.
     */
    fun getTopP(): Float {
        return userPreferencesRepository.getTopP()
    }
    
    /**
     * Set the top_p parameter.
     */
    fun setTopP(topP: Float) {
        userPreferencesRepository.setTopP(topP)
    }
    
    /**
     * Get the current top_k parameter.
     */
    fun getTopK(): Int {
        return userPreferencesRepository.getTopK()
    }
    
    /**
     * Set the top_k parameter.
     */
    fun setTopK(topK: Int) {
        userPreferencesRepository.setTopK(topK)
    }
    
    /**
     * Get the current context length parameter.
     */
    fun getContextLength(): Int {
        return userPreferencesRepository.getContextLength()
    }
    
    /**
     * Set the context length parameter.
     */
    fun setContextLength(contextLength: Int) {
        userPreferencesRepository.setContextLength(contextLength)
    }
    
    /**
     * Apply a parameter preset (Conservative, Balanced, or Creative).
     */
    fun applyParameterPreset(preset: ParameterPreset) {
        when (preset) {
            ParameterPreset.CONSERVATIVE -> {
                setTemperature(0.5f)
                setTopP(0.7f)
                setTopK(20)
            }
            ParameterPreset.BALANCED -> {
                setTemperature(1.0f)
                setTopP(0.9f)
                setTopK(40)
            }
            ParameterPreset.CREATIVE -> {
                setTemperature(1.5f)
                setTopP(0.95f)
                setTopK(60)
            }
        }
    }
    
    /**
     * Reset all parameters to their default values.
     */
    fun resetParametersToDefaults() {
        userPreferencesRepository.resetParametersToDefaults()
    }

}

/**
 * Enum defining parameter presets for quick configuration.
 */
enum class ParameterPreset {
    CONSERVATIVE,
    BALANCED,
    CREATIVE
}

fun sentThreadsValue(){

}