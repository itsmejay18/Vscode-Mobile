package com.customvscode.mobile.runtime

enum class RuntimeStatus { NOT_INSTALLED, DOWNLOADING, INSTALLING, INSTALLED, UPDATE_AVAILABLE, ERROR }

data class RuntimeInfo(
    val id: String,
    val name: String,
    val version: String?,
    val status: RuntimeStatus,
    val installDir: String?,
    val sizeBytes: Long?,
    val error: String?
)

data class FrameworkDefinition(
    val id: String,
    val name: String,
    val description: String,
    val requiredRuntimes: List<String>,
    val optionalRuntimes: List<String>
)
