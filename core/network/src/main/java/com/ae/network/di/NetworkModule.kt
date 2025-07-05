package com.ae.network.di

import com.ae.network.api.TmdbApiService
import com.ae.network.client.KtorClient
import com.ae.network.repository.TmdbRepository
import io.ktor.client.*
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule = module {
    // Ktor HttpClient
    single<HttpClient> {
        KtorClient.create()
    }

    // API configuration
    single(named("TmdbBaseUrl")) {
        "https://api.themoviedb.org/3"
    }

    single(named("TmdbApiKey")) {
        "61259bb22402b7eb23fd1ad78f0887de"
    }

    // API Service
    single {
        TmdbApiService(
            client = get(),
            baseUrl = get(named("TmdbBaseUrl")),
            apiKey = get(named("TmdbApiKey"))
        )
    }

    // Repository
    single {
        TmdbRepository(apiService = get())
    }
}