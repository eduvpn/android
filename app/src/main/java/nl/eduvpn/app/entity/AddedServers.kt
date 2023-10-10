package nl.eduvpn.app.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer

@Serializable
data class AddedServers(
    @SerialName("custom_servers")
    val customServers: List<AddedServer>? = emptyList(),
    @SerialName("secure_internet_server")
    val secureInternetServer: AddedServer? = null,
    @SerialName("institute_access_servers")
    val instituteAccessServers: List<AddedServer>? = emptyList()
) {
    fun hasServers() : Boolean {
        return !customServers.isNullOrEmpty() || secureInternetServer != null || !instituteAccessServers.isNullOrEmpty()
    }

    fun asInstances() : List<Instance> {
        val result = mutableListOf<Instance>()
        result += customServers?.map {
            Instance(
                baseURI = it.identifier,
                displayName = it.displayName,
                authorizationType = AuthorizationType.Organization,
                countryCode = it.countryCode,
                supportContact = it.supportContacts,
                isCustom = true
            )
        } ?: emptyList()
        result += instituteAccessServers?.map {
            Instance(
                baseURI = it.identifier,
                displayName = it.displayName,
                authorizationType = AuthorizationType.Local,
                countryCode = it.countryCode,
                supportContact = it.supportContacts,
                isCustom = false
            )
        } ?: emptyList()
        secureInternetServer?.let {
            result +=  listOf(Instance(
                baseURI = it.identifier,
                displayName = it.displayName,
                authorizationType = AuthorizationType.Distributed,
                countryCode = it.countryCode,
                supportContact = it.supportContacts,
                isCustom = false
            ))
        }
        return result
    }
}

@Serializable
data class AddedServer(
    @SerialName("display_name")
    @Serializable(with = TranslatableStringSerializer::class)
    val displayName: TranslatableString,
    @SerialName("identifier")
    val identifier: String,
    @SerialName("country_code")
    val countryCode: String? = null,
    @SerialName("support_contacts")
    val supportContacts: List<String> = emptyList()
)