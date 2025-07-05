package com.ae.network.client

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.observer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.*


object KtorClient {

    fun create(): HttpClient {
        return HttpClient(Android) {
            // JSON serialization
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }

            // Logging
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("Ktor", message)
                    }
                }
                level = LogLevel.INFO
            }

            // Response observer
            install(ResponseObserver) {
                onResponse { response ->
                    Log.d("Ktor", "Response: ${response.status}")
                }
            }

            // Timeout configuration
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }

            // Default request configuration
            defaultRequest {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
            }

            // Response validation
            HttpResponseValidator {
                validateResponse { response ->
                    when (response.status.value) {
                        in 300..399 -> throw RedirectResponseException(response, response.bodyAsText())
                        in 400..499 -> throw ClientRequestException(response, response.bodyAsText())
                        in 500..599 -> throw ServerResponseException(response, response.bodyAsText())
                    }
                }

                handleResponseExceptionWithRequest { exception, request ->
                    when (exception) {
                        is ClientRequestException -> {
                            val statusCode = exception.response.status.value
                            val message = when (statusCode) {
                                401 -> "Invalid API key"
                                404 -> "Resource not found"
                                429 -> "Rate limit exceeded"
                                else -> "Client error: $statusCode"
                            }
                            Log.e("Ktor", "Client error for ${request.url}: $message", exception)
                        }
                        is ServerResponseException -> {
                            Log.e("Ktor", "Server error for ${request.url}: ${exception.response.status}", exception)
                        }
                        is RedirectResponseException -> {
                            Log.w("Ktor", "Redirect for ${request.url}: ${exception.response.status}")
                        }
                        else -> {
                            Log.e("Ktor", "Unknown error for ${request.url}", exception)
                        }
                    }
                }
            }

            // Retry configuration
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
            }
        }
    }
}