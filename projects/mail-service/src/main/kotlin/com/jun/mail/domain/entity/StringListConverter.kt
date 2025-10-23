package com.jun.mail.domain.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringListConverter: AttributeConverter<List<String>, String?> {
    override fun convertToDatabaseColumn(attribute: List<String>): String? {
        return if (attribute.isEmpty()) null else attribute.joinToString(",")
    }

    override fun convertToEntityAttribute(dbData: String?): List<String> {
        return if (dbData.isNullOrBlank()) {
            emptyList()
        } else {
            dbData.split(",").map { it.trim() }
        }
    }
}