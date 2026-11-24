package com.rncoding.testvineshield.core.domain.error

/** Stable display reason; product numbers are one-based when applicable. */
enum class SprayValidationReason {
    RecordIdentity,
    Version,
    Zone,
    Time,
    Positive,
    AreaUnit,
    WaterUnit,
    WaterVolume,
    Notes,
    LineCount,
    DuplicateLine,
    ProductName,
    ProductQuantity,
    ProductUnit,
    ProductRate,
    ProductReference,
    Conflict,
    LineIdentity,
    ActivityIdentity,
    DeleteReload,
    ExportSaveFirst,
    ExportReload,
    ExportTooLarge
}
