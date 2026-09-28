package com.rncoding.testvineshield.core.data.local.database.converters

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate

object LocalDateRoomConverters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate): String {
        return value.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String): LocalDate {
        return LocalDate.parse(value)
    }
}