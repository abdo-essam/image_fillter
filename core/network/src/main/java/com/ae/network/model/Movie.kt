package com.ae.network.model


data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val popularity: Double,
    val adult: Boolean,
    val genreIds: List<Int>,
    val originalLanguage: String,
    val originalTitle: String,
    val video: Boolean
)

data class MovieDetails(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val runtime: Int?,
    val genres: List<Genre>,
    val adult: Boolean,
    val budget: Long?,
    val homepage: String?,
    val imdbId: String?,
    val originalLanguage: String,
    val originalTitle: String,
    val popularity: Double,
    val productionCompanies: List<ProductionCompany>,
    val productionCountries: List<ProductionCountry>,
    val revenue: Long?,
    val spokenLanguages: List<SpokenLanguage>,
    val status: String,
    val tagline: String?,
    val video: Boolean
)

data class Genre(
    val id: Int,
    val name: String
)

data class ProductionCompany(
    val id: Int,
    val logoUrl: String?,
    val name: String,
    val originCountry: String
)

data class ProductionCountry(
    val code: String,
    val name: String
)

data class SpokenLanguage(
    val englishName: String,
    val code: String,
    val name: String
)