package nl.eduvpn.app.entity

import kotlinx.serialization.Serializable

@Serializable
data class CookieAndData(
    val data: String,
    val cookie: Int
)
