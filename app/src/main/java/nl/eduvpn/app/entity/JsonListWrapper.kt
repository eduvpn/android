package nl.eduvpn.app.entity

import kotlinx.serialization.Serializable

@Serializable
data class JsonListWrapper<T>(val data: List<T>)
