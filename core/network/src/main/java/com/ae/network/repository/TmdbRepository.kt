package com.ae.network.repository

import com.ae.network.api.TmdbApiService
import com.ae.network.model.Cast
import com.ae.network.model.Movie
import com.ae.network.model.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TmdbRepository(
    private val apiService: TmdbApiService
) {

    suspend fun getPopularMoviesPage(page: Int): Result<List<Movie>> {
        return try {
            val response = apiService.getPopularMovies(page)
            Result.success(response.results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularPeoplePage(page: Int): Result<List<Person>> {
        return try {
            val response = apiService.getPopularPeople(page)
            Result.success(response.results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrendingPeoplePage(timeWindow: String = "week", page: Int): Result<List<Person>> {
        return try {
            val response = apiService.getTrendingPeople(timeWindow, page)
            Result.success(response.results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTopRatedMoviesPage(page: Int): Result<List<Movie>> {
        return try {
            val response = apiService.getTopRatedMovies(page)
            Result.success(response.results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPopularMovies(page: Int = 1): Flow<List<Movie>> = flow {
        try {
            val response = apiService.getPopularMovies(page)
            emit(response.results)
        } catch (e: Exception) {
            throw e
        }
    }

    fun getPopularPeople(page: Int = 1): Flow<List<Person>> = flow {
        try {
            val response = apiService.getPopularPeople(page)
            emit(response.results)
        } catch (e: Exception) {
            throw e
        }
    }

    fun getMovieCredits(movieId: Int): Flow<List<Cast>> = flow {
        try {
            val response = apiService.getMovieCredits(movieId)
            emit(response.cast)
        } catch (e: Exception) {
            throw e
        }
    }

    companion object {
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        const val POSTER_SIZE_W500 = "w500"
        const val PROFILE_SIZE_W500 = "w500"
        const val BACKDROP_SIZE_W780 = "w780"

        fun getFullImageUrl(path: String?, size: String = POSTER_SIZE_W500): String {
            return if (path != null) {
                "$IMAGE_BASE_URL$size$path"
            } else {
                ""
            }
        }
    }
}