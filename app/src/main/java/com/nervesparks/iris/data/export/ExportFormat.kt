package com.nervesparks.iris.data.export

/**
 * Supported export formats for conversation data.
 */
enum class ExportFormat {
    /** Machine-readable JSON with full metadata */
    JSON,

    /** Human-readable Markdown with proper formatting */
    MARKDOWN,

    /** Simple text format for universal compatibility */
    PLAIN_TEXT,
}

/**
 * Result of an export operation.
 *
 * @property success Whether the export was successful
 * @property filePath Path to the exported file (if successful)
 * @property checksum SHA-256 checksum for integrity verification
 * @property error Error message (if failed)
 */
data class ExportResult(
    val success: Boolean,
    val filePath: String? = null,
    val checksum: String? = null,
    val error: String? = null,
)

/**
 * Progress information for long-running export operations.
 *
 * @property current Current item being processed
 * @property total Total items to process
 * @property message Status message
 */
data class ExportProgress(
    val current: Int,
    val total: Int,
    val message: String,
)
