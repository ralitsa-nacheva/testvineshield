package com.rncoding.testvineshield.core.presentation.formatting

import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit

internal fun formatBlockArea(area: Double?, unit: AreaUnit?): String =
    area?.let { "${formatPlainDecimal(it)} ${unit?.symbol ?: "(unit unknown)"}" } ?: "Unknown"
