package com.ae.network.api

import com.ae.network.model.MovieCredits
import com.ae.network.model.MovieResponse
import com.ae.network.model.PersonResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class TmdbApiService(
    private val client: HttpClient,
    private val baseUrl: String,
    private val apiKey: String
) {

    suspend fun getPopularMovies(page: Int = 1): MovieResponse {
        return client.get("$baseUrl/movie/popular") {
            parameter("api_key", apiKey)
            parameter("page", page)
        }.body()
    }

    suspend fun getPopularPeople(page: Int = 1): PersonResponse {
        return client.get("$baseUrl/person/popular") {
            parameter("api_key", apiKey)
            parameter("page", page)
        }.body()
    }

    suspend fun getTrendingPeople(timeWindow: String = "week", page: Int = 1): PersonResponse {
        return client.get("$baseUrl/trending/person/$timeWindow") {
            parameter("api_key", apiKey)
            parameter("page", page)
        }.body()
    }

    suspend fun getTopRatedMovies(page: Int = 1): MovieResponse {
        return client.get("$baseUrl/movie/top_rated") {
            parameter("api_key", apiKey)
            parameter("page", page)
        }.body()
    }

    suspend fun getNowPlayingMovies(page: Int = 1): MovieResponse {
        return client.get("$baseUrl/movie/now_playing") {
            parameter("api_key", apiKey)
            parameter("page", page)
        }.body()
    }

    suspend fun getMovieCredits(movieId: Int): MovieCredits {
        return client.get("$baseUrl/movie/$movieId/credits") {
            parameter("api_key", apiKey)
        }.body()
    }

    suspend fun discoverMovies(
        page: Int = 1,
        sortBy: String = "popularity.desc",
        includeAdult: Boolean = false
    ): MovieResponse {
        return client.get("$baseUrl/discover/movie") {
            parameter("api_key", apiKey)
            parameter("page", page)
            parameter("sort_by", sortBy)
            parameter("include_adult", includeAdult)
        }.body()
    }
}