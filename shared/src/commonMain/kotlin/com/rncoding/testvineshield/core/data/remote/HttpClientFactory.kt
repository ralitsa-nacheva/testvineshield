package com.rncoding.testvineshield.core.data.remote

import io.ktor.client.HttpClient

interface HttpClientFactory {
    fun create(): HttpClient
}