package com.sitesnag.tracker.domain.model

enum class RecordType(val displayName: String, val prefix: String) {
    NORMAL_OBSERVATION("Normal Observation", "OBS"),
    NCR("Non-Conformance Report (NCR)", "NCR")
}

enum class Priority(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

enum class Status(val displayName: String) {
    OPEN("Open"),
    RECTIFIED("Rectified")
}

enum class CustomFieldType(val displayName: String) {
    TEXT("Text"),
    NUMBER("Number"),
    DATE("Date"),
    DROPDOWN("Dropdown")
}

data class CustomFieldDefinition(
    val id: String,
    val name: String,
    val type: CustomFieldType,
    val options: List<String> = emptyList(),
    val isRequired: Boolean = false,
    val orderIndex: Int = 0
)

data class CustomFieldValue(
    val fieldId: String,
    val fieldName: String,
    val value: String
)
