package nl.eduvpn.app.entity

import kotlinx.serialization.Serializable

@Serializable
data class CookieAndStringData(
    val data: String,
    val cookie: Int
)
