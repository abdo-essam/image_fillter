package com.ae.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonResponse(
    @SerialName("page")
    val page: Int,
    @SerialName("results")
    val results: List<Person>,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_results")
    val totalResults: Int
)

@Serializable
data class Person(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("profile_path")
    val profilePath: String?,
    @SerialName("known_for_department")
    val knownForDepartment: String?,
    @SerialName("popularity")
    val popularity: Double
)