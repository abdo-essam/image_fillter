package com.ae.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MultiSearchResponse(
    @SerialName("page")
    val page: Int,
    @SerialName("results")
    val results: List<SearchResultDto>,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_results")
    val totalResults: Int
)

@Serializable
data class SearchResultDto(
    @SerialName("id")
    val id: Int,
    @SerialName("media_type")
    val mediaType: String,
    @SerialName("title")
    val title: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("overview")
    val overview: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("backdrop_path")
    val backdropPath: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double? = null,
    @SerialName("vote_count")
    val voteCount: Int? = null,
    @SerialName("adult")
    val adult: Boolean? = null,
    @SerialName("genre_ids")
    val genreIds: List<Int>? = null,
    @SerialName("original_language")
    val originalLanguage: String? = null,
    @SerialName("popularity")
    val popularity: Double? = null
)

data class SearchResult(
    val id: Int,
    val mediaType: String,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val adult: Boolean,
    val genreIds: List<Int>,
    val originalLanguage: String,
    val popularity: Double
)