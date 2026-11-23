package com.rncoding.testvineshield.core.domain.error

enum class VineyardField {
    NAME,
    SIZE,
    COUNTRY,
    CITY,
    LATITUDE,
    LONGITUDE,
    TIME_ZONE,
    ELEVATION
}

enum class VineyardValidationReason {
    NameRequired,
    NameTooLong,
    SizePositive,
    SizeNumber,
    CountryRequired,
    CityRequired,
    LatitudeRange,
    LongitudeRange,
    TimeZoneRequired,
    ElevationRange,
    LatitudeNumber,
    LongitudeNumber,
    ElevationNumber,
    TimeZoneInvalid
}

data class VineyardValidationError(
    val field: VineyardField,
    override val userMessage: String,
    override val debugMessage: String? = null,
    override val cause: Throwable? = null,
    val reason: VineyardValidationReason? = null
) : AppError