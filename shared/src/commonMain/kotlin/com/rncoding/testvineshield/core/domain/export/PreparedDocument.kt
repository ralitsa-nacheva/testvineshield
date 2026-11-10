package com.rncoding.testvineshield.core.domain.export

enum class DocumentExportFormat(val extension: String, val mimeType: String) {
    JSON("json", "application/json"), CSV("csv", "text/csv")
}

data class PreparedDocument(val fileName: String, val mimeType: String, val content: String, val recordCount: Int)
