package com.ae.network.client

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object KtorClient {

    fun create(): HttpClient {
        return HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }

            defaultRequest {
                headers.append("Accept", "application/json")
            }

            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, request ->
                    Log.e("Ktor", "Request failed: ${request.url}", exception)
                }
            }
        }
    }
}