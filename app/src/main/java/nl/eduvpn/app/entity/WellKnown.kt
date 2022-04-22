package nl.eduvpn.app.entity

import kotlinx.serialization.Serializable

@Serializable
data class WellKnown(val api: DiscoveredAPIs)
