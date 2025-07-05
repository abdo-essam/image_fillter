package com.ae.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieCredits(
    @SerialName("id")
    val id: Int,
    @SerialName("cast")
    val cast: List<Cast>
)

@Serializable
data class Cast(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("character")
    val character: String?,
    @SerialName("profile_path")
    val profilePath: String?
)