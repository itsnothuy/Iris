package android.llama.cpp

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LLamaAndroid {
    private val tag: String? = this::class.simpleName
    private var stopGeneration: Boolean = false
    //private var model_eot_str: String = ""

    private val threadLocalState: ThreadLocal<State> = ThreadLocal.withInitial { State.Idle }

    private val _isSending = mutableStateOf(false)
    private val isSending: Boolean by _isSending

    private val _isMarked = mutableStateOf(false)
    private val isMarked: Boolean by _isMarked

    private val _isCompleteEOT = mutableStateOf(true)
    private val isCompleteEOT: Boolean by _isCompleteEOT
    
    // Queue management
    private val maxQueueSize: Int = 3
    private val requestQueue = Channel<String>(capacity = maxQueueSize)
    private val queueMutex = Mutex()
    private val _queueSize = mutableStateOf(0)
    private val _isQueued = mutableStateOf(false)
    
    // Rate-limit and thermal state
    private val _isRateLimited = mutableStateOf(false)
    private val _isThermalThrottled = mutableStateOf(false)
    private var requestCount = 0
    private var lastResetTime = System.currentTimeMillis()
    private val rateLimitWindow = 60_000L // 1 minute window
    private val maxRequestsPerWindow = 10 // Max 10 requests per minute

    fun getIsSending(): Boolean {
        return isSending
    }


    fun getIsMarked(): Boolean {
        return isMarked
    }

    fun getIsCompleteEOT(): Boolean {
        return isCompleteEOT
    }
    
    fun getQueueSize(): Int {
        return _queueSize.value
    }
    
    fun isQueued(): Boolean {
        return _isQueued.value
    }
    
    fun isRateLimited(): Boolean {
        return _isRateLimited.value
    }
    
    fun isThermalThrottled(): Boolean {
        return _isThermalThrottled.value
    }
    
    fun setThermalState(isThrottled: Boolean) {
        _isThermalThrottled.value = isThrottled
        if (isThrottled) {
            Log.w(tag, "Thermal throttling activated")
        }
    }

    fun stopTextGeneration() {
        _isSending.value = false

        stopGeneration = true
        _isMarked.value = false
    }


    private val runLoop: CoroutineDispatcher = Executors.newSingleThreadExecutor {
        thread(start = false, name = "Llm-RunLoop") {
            Log.d(tag, "Dedicated thread for native code: ${Thread.currentThread().name}")

            // No-op if called more than once.
            System.loadLibrary("llama-android")

            // Set llama log handler to Android
            log_to_android()
            backend_init(false)

            Log.d(tag, system_info())

            it.run()
        }.apply {
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, exception: Throwable ->
                Log.e(tag, "Unhandled exception", exception)
            }
        }
    }.asCoroutineDispatcher()

    private val nlen: Int = 1024
    private val context_size: Int = 4096

    private external fun log_to_android()
    private external fun load_model(filename: String): Long
    private external fun free_model(model: Long)
    private external fun new_context(model: Long, userThreads: Int): Long
    private external fun free_context(context: Long)
    private external fun backend_init(numa: Boolean)
    private external fun backend_free()
    private external fun new_batch(nTokens: Int, embd: Int, nSeqMax: Int): Long
    private external fun free_batch(batch: Long)
    private external fun new_sampler(top_p: Float, top_k: Int, temp: Float): Long
    private external fun free_sampler(sampler: Long)
    private external fun bench_model(
        context: Long,
        model: Long,
        batch: Long,
        pp: Int,
        tg: Int,
        pl: Int,
        nr: Int
    ): String

    private external fun system_info(): String

    private external fun completion_init(
        context: Long,
        batch: Long,
        text: String,
        nLen: Int
    ): Int

    private external fun oaicompat_completion_param_parse(
        allmessages: Array<Map<String, String>>,
        model: Long
    ): String

    private external fun completion_loop(
        context: Long,
        batch: Long,
        sampler: Long,
        nLen: Int,
        ncur: IntVar
    ): String?

    private external fun kv_cache_clear(context: Long)

    private external fun get_eot_str(model: Long): String



    suspend fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1): String {
        return withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    Log.d(tag, "bench(): $state")
                    bench_model(state.context, state.model, state.batch, pp, tg, pl, nr)
                }

                else -> throw IllegalStateException("No model loaded")
            }
        }
    }

    suspend fun load(pathToModel: String, userThreads: Int, topK: Int, topP: Float, temp: Float){
        withContext(runLoop) {
            when (threadLocalState.get()) {
                is State.Idle -> {
                    val model = load_model(pathToModel)
                    if (model == 0L)  throw IllegalStateException("load_model() failed")

                    val context = new_context(model, userThreads)
                    if (context == 0L) throw IllegalStateException("new_context() failed")

                    val batch = new_batch(4096, 0, 1)
                    if (batch == 0L) throw IllegalStateException("new_batch() failed")

                    val sampler = new_sampler(top_k = topK, top_p = topP, temp = temp)
                    if (sampler == 0L) throw IllegalStateException("new_sampler() failed")


                    val modelEotStr = get_eot_str(model)
                    if (modelEotStr == "") throw IllegalStateException("eot_fetch() failed")

                    Log.i(tag, "Loaded model $pathToModel")
                    threadLocalState.set(State.Loaded(model, context, batch, sampler, modelEotStr))
                }
                else -> {
                    throw IllegalStateException("Model already loaded")
                }
            }
        }
    }


    suspend fun getTemplate(messages: List<Map<String, String>>): String {
        var data = ""
        withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    val arrayMessages = messages.toTypedArray() // Convert list to array for JNI compatibility
                    data = oaicompat_completion_param_parse(
                        allmessages = arrayMessages,
                        model = state.model
                    )
                }
                else -> {}
            }
        }

        return data
    }
    
    /**
     * Try to enqueue a message for processing.
     * Returns true if successfully queued or sent immediately, false if queue is full.
     */
    suspend fun tryEnqueue(message: String): Boolean {
        return queueMutex.withLock {
            // Check rate limit
            checkRateLimit()
            
            if (_isSending.value) {
                // A message is currently being processed
                if (_queueSize.value >= maxQueueSize) {
                    Log.w(tag, "Queue is full (size: ${_queueSize.value}), rejecting message")
                    false
                } else {
                    val queued = requestQueue.trySend(message).isSuccess
                    if (queued) {
                        _queueSize.value += 1
                        _isQueued.value = true
                        Log.i(tag, "Message queued (queue size: ${_queueSize.value})")
                    }
                    queued
                }
            } else {
                // No message being processed, can send immediately
                true
            }
        }
    }
    
    /**
     * Check if we've exceeded the rate limit and update state accordingly.
     */
    private fun checkRateLimit() {
        val currentTime = System.currentTimeMillis()
        
        // Reset counter if window has passed
        if (currentTime - lastResetTime > rateLimitWindow) {
            requestCount = 0
            lastResetTime = currentTime
            _isRateLimited.value = false
        }
        
        requestCount++
        
        // Check if rate limit exceeded
        if (requestCount > maxRequestsPerWindow) {
            _isRateLimited.value = true
            Log.w(tag, "Rate limit exceeded: $requestCount requests in ${currentTime - lastResetTime}ms")
        } else {
            _isRateLimited.value = false
        }
    }
    
    /**
     * Get the next queued message, if any.
     */
    private suspend fun dequeue(): String? {
        return queueMutex.withLock {
            if (_queueSize.value > 0) {
                val message = requestQueue.tryReceive().getOrNull()
                if (message != null) {
                    _queueSize.value -= 1
                    if (_queueSize.value == 0) {
                        _isQueued.value = false
                    }
                    Log.i(tag, "Message dequeued (queue size: ${_queueSize.value})")
                }
                message
            } else {
                null
            }
        }
    }

    suspend fun send(message: String): Flow<String> = flow {
        stopGeneration = false
        _isSending.value = true
        
        // Calculate streaming delay based on rate-limit and thermal state
        val streamDelay = calculateStreamDelay()
        if (streamDelay > 0) {
            Log.i(tag, "Applying streaming delay of ${streamDelay}ms due to rate-limit or thermal throttle")
        }
        
        when (val state = threadLocalState.get()) {
            is State.Loaded -> {
                val ncur = IntVar(completion_init(state.context, state.batch, message, nlen))
                var end_token_store = ""
                var chat_len = 0
                while (chat_len <= nlen && ncur.value < context_size && !stopGeneration) {
                    _isSending.value = true
                    val str = completion_loop(state.context, state.batch, state.sampler, nlen, ncur)
                    chat_len += 1
                    if (str == "```" || str == "``") {
                        _isMarked.value = !_isMarked.value
                    }
                    if (str == null) {
                        _isSending.value = false
                        _isCompleteEOT.value = true
                        break
                    }
                    end_token_store = end_token_store+str
                    if((end_token_store.length > state.modelEotStr.length) and end_token_store.contains(state.modelEotStr)){
                        _isSending.value = false
                        _isCompleteEOT.value = false
                        break
                    }
                    if((end_token_store.length/2) > state.modelEotStr.length ){
                        end_token_store = end_token_store.slice(end_token_store.length/2..end_token_store.length-1)
                    }


//                    if (str == "</s>" || str == " User" || str== " user" || str == "user" || str == "<|im_end|>" || str == "\n" +
//                        "                                                                                                    "
//                    ) {
//
//                        _isSending.value = false
//                        break
//
//                    }
                    if (stopGeneration) {
                        break
                    }
                    
                    // Apply degradation delay if needed
                    if (streamDelay > 0) {
                        delay(streamDelay)
                    }
                    
                    emit(str)
                }
                kv_cache_clear(state.context)
            }
            else -> {
                _isSending.value = false
            }
        }
        _isSending.value = false
    }.flowOn(runLoop)
    
    /**
     * Calculate streaming delay in milliseconds based on rate-limit and thermal state.
     * Returns higher delay when rate-limited or thermally throttled.
     */
    private fun calculateStreamDelay(): Long {
        return when {
            _isThermalThrottled.value && _isRateLimited.value -> 150L // Both conditions: 150ms
            _isThermalThrottled.value -> 100L // Thermal only: 100ms
            _isRateLimited.value -> 75L // Rate-limit only: 75ms
            else -> 0L // Normal: no delay
        }
    }



    suspend fun myCustomBenchmark(): Flow<String> = flow {
        try {
            withTimeout(30.seconds) { // Set timeout to 2 minutes
                when (val state = threadLocalState.get()) {
                    is State.Loaded -> {
                        val ncur = IntVar(completion_init(state.context, state.batch, "Write an article on global warming in 1000 words", nlen))
                        while (ncur.value <= nlen) {
                            val str = completion_loop(state.context, state.batch, state.sampler, nlen, ncur)
                            if (str == null) {
                                _isSending.value = false
                                _isCompleteEOT.value = true
                                break
                            }
                            if (stopGeneration) {
                                break
                            }
                            emit(str)
                        }
                        kv_cache_clear(state.context)
                    }
                    else -> {
                        _isSending.value = false
                    }
                }
            }
        } catch (e: Exception) {
            // Handle timeout or any other exceptions if necessary
            if (e is kotlinx.coroutines.TimeoutCancellationException) {
                println("Benchmark timed out after 2 minutes.")
            }
        } finally {
            _isSending.value = false
        }
    }.flowOn(runLoop)





    /**
     * Unloads the model and frees resources.
     *
     * This is a no-op if there's no model loaded.
     */
    suspend fun unload() {
        withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    free_context(state.context)
                    free_model(state.model)
                    free_sampler(state.sampler)
                    free_batch(state.batch)

                    threadLocalState.set(State.Idle)
                }
                else -> {}
            }
        }
    }

    fun send_eot_str(): String {

        return when (val state = threadLocalState.get()) {
            is State.Loaded -> {
                state.modelEotStr
            }

            else -> {
                "<|im_end|>"
            }
        }

    }

    companion object {
        private class IntVar(value: Int) {
            @Volatile
            var value: Int = value
                private set

            fun inc() {
                synchronized(this) {
                    value += 1
                }
            }
        }

        private sealed interface State {
            data object Idle: State
            data class Loaded(val model: Long, val context: Long, val batch: Long, val sampler: Long , val modelEotStr:String): State
        }

        // Enforce only one instance of Llm.
        private val _instance: LLamaAndroid = LLamaAndroid()

        fun instance(): LLamaAndroid = _instance
    }
}
