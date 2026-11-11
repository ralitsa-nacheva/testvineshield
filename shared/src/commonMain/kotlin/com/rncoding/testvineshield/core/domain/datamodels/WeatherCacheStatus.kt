package com.rncoding.testvineshield.core.domain.datamodels

data class WeatherCacheStatus(val kind: String,val state: String,val attemptedAt: Long,val succeededAt: Long?,
    val coverageStart: Long?,val coverageEnd: Long?,val hours: Int,val errorCode: String?)
