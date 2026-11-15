package com.sitesnag.tracker

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sitesnag.tracker.data.local.converter.Converters
import com.sitesnag.tracker.domain.model.CustomFieldType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CustomFieldSerializationTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    @Test
    fun testCustomFieldValuesMapSerialization() {
        val map = mapOf(
            "Drawing / Sheet Reference" to "DWG-STR-401 Rev B",
            "Inspection Stage" to "Post-Demolding",
            "Subcontractor Package" to "Package B - Core & Shell",
            "Numeric Tolerance (mm)" to "3.5"
        )

        val json = converters.fromStringMap(map)
        assertNotNull(json)
        assertTrue(json.contains("DWG-STR-401 Rev B"))
        assertTrue(json.contains("Inspection Stage"))

        val deserialized = converters.toStringMap(json)
        assertEquals(4, deserialized.size)
        assertEquals("DWG-STR-401 Rev B", deserialized["Drawing / Sheet Reference"])
        assertEquals("Post-Demolding", deserialized["Inspection Stage"])
        assertEquals("3.5", deserialized["Numeric Tolerance (mm)"])
    }

    @Test
    fun testEmptyAndNullMapSerialization() {
        val emptyJson = converters.fromStringMap(emptyMap())
        assertEquals("{}", emptyJson)

        val fromNull = converters.toStringMap(null)
        assertTrue(fromNull.isEmpty())

        val fromBlank = converters.toStringMap("   ")
        assertTrue(fromBlank.isEmpty())

        val fromMalformed = converters.toStringMap("{not: valid: json}")
        assertTrue(fromMalformed.isEmpty())
    }

    @Test
    fun testDropdownOptionsListSerialization() {
        val options = listOf("Pre-Pour Inspection", "Post-Demolding", "First Fix", "Handover Punch")
        val json = converters.fromStringList(options)
        assertTrue(json.contains("Pre-Pour Inspection"))

        val deserialized = converters.toStringList(json)
        assertEquals(4, deserialized.size)
        assertEquals("Pre-Pour Inspection", deserialized[0])
        assertEquals("Handover Punch", deserialized[3])
    }

    @Test
    fun testCustomFieldTypeConverter() {
        assertEquals("DROPDOWN", converters.fromCustomFieldType(CustomFieldType.DROPDOWN))
        assertEquals(CustomFieldType.DROPDOWN, converters.toCustomFieldType("DROPDOWN"))
        assertEquals(CustomFieldType.TEXT, converters.toCustomFieldType("TEXT"))
        assertEquals(CustomFieldType.NUMBER, converters.toCustomFieldType("NUMBER"))
        assertEquals(CustomFieldType.DATE, converters.toCustomFieldType("DATE"))
        assertEquals(CustomFieldType.TEXT, converters.toCustomFieldType("UNKNOWN_FALLBACK"))
    }
}
