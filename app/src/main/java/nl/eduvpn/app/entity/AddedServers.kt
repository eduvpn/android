package nl.eduvpn.app.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer

@Serializable
data class AddedServers(
    @SerialName("custom_servers")
    val customServers: List<AddedCustomServer>? = emptyList(),
    @SerialName("secure_internet_server")
    val secureInternetServer: Organization? = null
) {
    fun hasServers() : Boolean {
        return !customServers.isNullOrEmpty() || secureInternetServer != null
    }

    fun asInstances() : List<Instance> {
        return customServers?.map {
            Instance(
                baseURI = it.identifier,
                displayName = it.displayName,
                authorizationType = AuthorizationType.Local,
                isCustom = true
            )
        } ?: emptyList()
    }
}

@Serializable
data class AddedCustomServer(
    @SerialName("display_name")
    @Serializable(with = TranslatableStringSerializer::class)
    val displayName: TranslatableString,
    @SerialName("identifier")
    val identifier: String,
)