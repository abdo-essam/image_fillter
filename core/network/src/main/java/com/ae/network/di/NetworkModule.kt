package com.ae.network.di

import com.ae.network.api.TmdbApiService
import com.ae.network.client.KtorClient
import com.ae.network.repository.TmdbRepository
import org.koin.dsl.module

val networkModule = module {
    single { KtorClient.create() }

    single {
        TmdbApiService(
            client = get(),
            baseUrl = "https://api.themoviedb.org/3",
            apiKey = "61259bb22402b7eb23fd1ad78f0887de"
        )
    }

    single { TmdbRepository(get()) }
}