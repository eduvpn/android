package nl.eduvpn.app.entity

import kotlinx.serialization.Serializable

@Serializable
data class CookieAndStringData(
    val data: String,
    val cookie: Int
)

@Serializable
data class CookieAndStringArrayData(
    val data: List<String>,
    val cookie: Int
)