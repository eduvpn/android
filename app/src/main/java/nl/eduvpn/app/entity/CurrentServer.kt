package nl.eduvpn.app.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer
import org.eduvpn.common.ServerType

@Serializable
data class CurrentServer(
    @SerialName("custom_server")
    val customServer: CurrentServerInfo? = null,
    @SerialName("institute_access_server")
    val instituteAccessServer: CurrentServerInfo? = null,
    @SerialName("secure_internet_server")
    val secureInternetServer: CurrentServerInfo? = null,
    @SerialName("server_type")
    val serverType: Int?
) {

    private val info : CurrentServerInfo? = customServer ?: instituteAccessServer ?: secureInternetServer
    fun getUniqueId(): String? {
        return info?.identifier
    }

    fun getDisplayName(): TranslatableString? {
        return info?.displayName
    }

    fun getProfiles(): List<Profile>? {
        return info?.profiles?.getProfileList()
    }

    val currentProfile: Profile? = info?.profiles?.currentProfile

    fun asInstance() : Instance? {
        val authorizationType = when (serverType) {
            ServerType.Custom.nativeValue -> AuthorizationType.Organization
            ServerType.SecureInternet.nativeValue  -> AuthorizationType.Distributed
            ServerType.InstituteAccess.nativeValue  -> AuthorizationType.Local
            else -> return null
        }
        return Instance(
            baseURI = getUniqueId() ?: return null,
            displayName = getDisplayName() ?: TranslatableString(),
            authorizationType = authorizationType
        )
    }
}

@Serializable
data class CurrentServerInfo (
    val identifier: String,

    @SerialName("display_name")
    @Serializable(with = TranslatableStringSerializer::class)
    val displayName: TranslatableString,

    val profiles: ProfileWithoutIdMap? = null
)


