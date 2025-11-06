package com.nervesparks.iris.common.error

/**
 * Base exception for all Iris application errors
 */
open class IrisException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when model operations fail
 */
class ModelException(
    message: String,
    cause: Throwable? = null
) : IrisException(message, cause)

/**
 * Exception thrown when hardware operations fail
 */
class HardwareException(
    message: String,
    cause: Throwable? = null
) : IrisException(message, cause)

/**
 * Exception thrown when safety checks fail
 */
class SafetyException(
    message: String,
    cause: Throwable? = null
) : IrisException(message, cause)

/**
 * Exception thrown when RAG operations fail
 */
class RAGException(
    message: String,
    cause: Throwable? = null
) : IrisException(message, cause)

/**
 * Exception thrown when ASR operations fail
 */
class ASRException(
    message: String,
    cause: Throwable? = null
) : IrisException(message, cause)

/**
 * Exception thrown when vision operations fail
 */
class VisionException(
    message: String,
    cause: Throwable? = null
) : IrisException(message, cause)

/**
 * Exception thrown when tool execution fails
 */
class ToolException(
    message: String,
    cause: Throwable? = null
) : IrisException(message, cause)
