package com.rncoding.testvineshield.core.data.local.database.converters

import androidx.room.TypeConverter
import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import com.rncoding.testvineshield.core.domain.datamodels.units.LengthUnit

class MeasurementUnitRoomConverters {
    @TypeConverter fun areaToCode(unit: AreaUnit?): String? = unit?.code
    @TypeConverter fun codeToArea(code: String?): AreaUnit? = code?.let { AreaUnit.fromCode(it) }
    @TypeConverter fun lengthToCode(unit: LengthUnit?): String? = unit?.code
    @TypeConverter fun codeToLength(code: String?): LengthUnit? = code?.let { LengthUnit.fromCode(it) }
}
