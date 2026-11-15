package com.sitesnag.tracker.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sitesnag.tracker.domain.model.CustomFieldType
import com.sitesnag.tracker.domain.model.Priority
import com.sitesnag.tracker.domain.model.RecordType
import com.sitesnag.tracker.domain.model.Status

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromRecordType(value: RecordType?): String? = value?.name

    @TypeConverter
    fun toRecordType(value: String?): RecordType? = value?.let {
        try { RecordType.valueOf(it) } catch (e: Exception) { RecordType.NORMAL_OBSERVATION }
    }

    @TypeConverter
    fun fromPriority(value: Priority?): String? = value?.name

    @TypeConverter
    fun toPriority(value: String?): Priority? = value?.let {
        try { Priority.valueOf(it) } catch (e: Exception) { Priority.MEDIUM }
    }

    @TypeConverter
    fun fromStatus(value: Status?): String? = value?.name

    @TypeConverter
    fun toStatus(value: String?): Status? = value?.let {
        try { Status.valueOf(it) } catch (e: Exception) { Status.OPEN }
    }

    @TypeConverter
    fun fromCustomFieldType(value: CustomFieldType?): String? = value?.name

    @TypeConverter
    fun toCustomFieldType(value: String?): CustomFieldType? = value?.let {
        try { CustomFieldType.valueOf(it) } catch (e: Exception) { CustomFieldType.TEXT }
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String {
        return if (map == null) "{}" else gson.toJson(map)
    }

    @TypeConverter
    fun toStringMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return if (list == null) "[]" else gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
