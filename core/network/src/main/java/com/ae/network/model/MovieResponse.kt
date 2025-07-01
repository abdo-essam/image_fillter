package com.ae.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MoviesResponse(
    @SerialName("page")
    val page: Int,
    @SerialName("results")
    val results: List<MovieDto>,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_results")
    val totalResults: Int
)

@Serializable
data class MovieDto(
    @SerialName("id")
    val id: Int,
    @SerialName("title")
    val title: String,
    @SerialName("overview")
    val overview: String,
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("backdrop_path")
    val backdropPath: String?,
    @SerialName("release_date")
    val releaseDate: String,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("vote_count")
    val voteCount: Int,
    @SerialName("popularity")
    val popularity: Double,
    @SerialName("adult")
    val adult: Boolean,
    @SerialName("genre_ids")
    val genreIds: List<Int>,
    @SerialName("original_language")
    val originalLanguage: String,
    @SerialName("original_title")
    val originalTitle: String,
    @SerialName("video")
    val video: Boolean
)

@Serializable
data class MovieDetailsDto(
    @SerialName("id")
    val id: Int,
    @SerialName("title")
    val title: String,
    @SerialName("overview")
    val overview: String,
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("backdrop_path")
    val backdropPath: String?,
    @SerialName("release_date")
    val releaseDate: String,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("vote_count")
    val voteCount: Int,
    @SerialName("runtime")
    val runtime: Int?,
    @SerialName("genres")
    val genres: List<GenreDto>,
    @SerialName("adult")
    val adult: Boolean,
    @SerialName("budget")
    val budget: Long? = 0,
    @SerialName("homepage")
    val homepage: String? = null,
    @SerialName("imdb_id")
    val imdbId: String? = null,
    @SerialName("original_language")
    val originalLanguage: String,
    @SerialName("original_title")
    val originalTitle: String,
    @SerialName("popularity")
    val popularity: Double,
    @SerialName("production_companies")
    val productionCompanies: List<ProductionCompanyDto> = emptyList(),
    @SerialName("production_countries")
    val productionCountries: List<ProductionCountryDto> = emptyList(),
    @SerialName("revenue")
    val revenue: Long? = 0,
    @SerialName("spoken_languages")
    val spokenLanguages: List<SpokenLanguageDto> = emptyList(),
    @SerialName("status")
    val status: String,
    @SerialName("tagline")
    val tagline: String? = null,
    @SerialName("video")
    val video: Boolean
)

@Serializable
data class GenreDto(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String
)

@Serializable
data class ProductionCompanyDto(
    @SerialName("id")
    val id: Int,
    @SerialName("logo_path")
    val logoPath: String?,
    @SerialName("name")
    val name: String,
    @SerialName("origin_country")
    val originCountry: String
)

@Serializable
data class ProductionCountryDto(
    @SerialName("iso_3166_1")
    val iso31661: String,
    @SerialName("name")
    val name: String
)

@Serializable
data class SpokenLanguageDto(
    @SerialName("english_name")
    val englishName: String,
    @SerialName("iso_639_1")
    val iso6391: String,
    @SerialName("name")
    val name: String
)