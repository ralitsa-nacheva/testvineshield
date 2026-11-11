package com.rncoding.testvineshield.core.domain.error

data object WeatherCacheSourceChanged : AppError {
    override val userMessage="The vineyard location or refresh request changed. Refresh again."
    override val debugMessage="Stale weather request rejected before commit"
    override val cause: Throwable?=null
}
