package nl.eduvpn.app.service

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.entity.AddedServers
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.CertExpiryTimes
import nl.eduvpn.app.entity.CurrentServer
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.entity.SerializedVpnConfig
import nl.eduvpn.app.entity.exception.CommonException
import nl.eduvpn.app.service.SerializerService.UnknownFormatException
import nl.eduvpn.app.utils.Log
import org.eduvpn.common.GoBackend
import org.eduvpn.common.GoBackend.Callback
import org.eduvpn.common.ServerType
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale


class BackendService(
    private val context: Context,
    private val serializerService: SerializerService,
    private val preferencesService: PreferencesService
) {

    companion object {
        private const val DIRECTORY_BACKEND_CONFIG_FILES = "backend_config_files"
        private const val ERROR_EMPTY_RESPONSE = "Empty response returned by common module"

        private val TAG = BackendService::class.java.simpleName
    }

    enum class State(val nativeValue: Int) {
        ASK_LOCATION(2),
        OAUTH_STARTED(6),
        ASK_PROFILE(9)
    }

    private val goBackend = GoBackend()
    private var pendingOAuthCookie: Int? = null
    private var pendingProfileSelectionCookie: Int? = null
    var lastSelectedProfile: String? = null
        private set

    private var onConfigReady: ((SerializedVpnConfig, Boolean) -> Unit)? = null

    fun register(
        startOAuth: (String) -> Unit,
        selectProfiles: (List<Profile>) -> Unit,
        selectCountry: (Int?) -> Unit,
        connectWithConfig: (SerializedVpnConfig, Boolean) -> Unit,
        showError: (Throwable) -> Unit
    ): String? {
        onConfigReady = { config, forceTcp ->
            connectWithConfig(config, forceTcp)
        }
        GoBackend.callbackFunction = object : Callback {

            // The library wants to get a token from our internal storage
            override fun getToken(serverJson: String): String? {
                // Find out the serverId
                val parsedServer = serializerService.deserializeCurrentServer(serverJson)
                parsedServer.getUniqueId()?.let { uniqueId ->
                    return preferencesService.getToken(uniqueId)
                }
                return null
            }

            // The library wants to save a token in our internal storage
            override fun setToken(serverJson: String, token: String?) {
                // Find out the serverId
                val parsedServer = serializerService.deserializeCurrentServer(serverJson)
                parsedServer.getUniqueId()?.let { uniqueId ->
                    preferencesService.setToken(uniqueId, token)
                }
            }

            // Called when the native state machine changes
            override fun onNewState(newState: Int, data: String?): Boolean {
                return if (newState == State.OAUTH_STARTED.nativeValue) {
                    if (data.isNullOrEmpty()) {
                        showError(CommonException(ERROR_EMPTY_RESPONSE))
                        return true
                    }
                    val cookieAndData = serializerService.deserializeCookieAndStringData(data)
                    pendingOAuthCookie = cookieAndData.cookie
                    startOAuth(cookieAndData.data)
                    true
                } else if (newState == State.ASK_PROFILE.nativeValue) {
                    if (data.isNullOrEmpty()) {
                        showError(CommonException(ERROR_EMPTY_RESPONSE))
                        return true
                    }
                    val cookieAndData =
                        serializerService.deserializeCookieAndCookieAndProfileListData(data)
                    pendingProfileSelectionCookie = cookieAndData.cookie
                    selectProfiles(cookieAndData.data.getProfileList())
                    true
                } else if (newState == State.ASK_LOCATION.nativeValue) {
                    if (data.isNullOrEmpty()) {
                        showError(CommonException(ERROR_EMPTY_RESPONSE))
                        return true
                    }
                    val cookieAndData = serializerService.deserializeCookieAndStringData(data)
                    selectCountry(cookieAndData.cookie)
                    true
                } else {
                    false
                }
            }
        }
        val version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        var configFilesDir: String? = null
        try {
            context.cacheDir?.let {
                if (it.exists()) {
                    val configDirectory = File(context.cacheDir, DIRECTORY_BACKEND_CONFIG_FILES)
                    if (!configDirectory.exists()) {
                        configDirectory.mkdirs()
                    }
                    if (configDirectory.exists()) {
                        configFilesDir = configDirectory.absolutePath
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Could not create files dir for Go backend", ex)
        }
        return goBackend.register(
            BuildConfig.OAUTH_CLIENT_ID,
            version,
            configFilesDir,
            BuildConfig.DEBUG
        )
    }

    fun deregister() {
        val errorString = goBackend.deregister()
        if (errorString != null) {
            Log.w(TAG, "Unable to deregister Go backend: $errorString")
        }
        GoBackend.callbackFunction = null
        onConfigReady = null
    }

    @Throws(CommonException::class)
    fun discoverOrganizations(): String {
        val dataWithError = goBackend.discoverOrganizations()
        if (dataWithError.isError) {
            throw CommonException(dataWithError.error)
        }
        if (dataWithError.data.isNullOrEmpty()) {
            throw CommonException(ERROR_EMPTY_RESPONSE)
        }
        return dataWithError.data!!
    }

    @Throws(CommonException::class)
    fun discoverServers(): String {
        val dataWithError = goBackend.discoverServers()
        if (dataWithError.isError) {
            throw CommonException(dataWithError.error)
        }
        if (dataWithError.data.isNullOrEmpty()) {
            throw CommonException(ERROR_EMPTY_RESPONSE)
        }
        return dataWithError.data!!
    }

    @kotlin.jvm.Throws(CommonException::class)
    suspend fun addServer(instance: Instance) {
        val errorString = goBackend.addServer(
            instance.authorizationType.toNativeServerType().nativeValue,
            instance.baseURI
        )
        if (!errorString.isNullOrEmpty()) {
            throw CommonException(errorString)
        }
    }

    @kotlin.jvm.Throws(CommonException::class)
    fun getCertExpiryTimes():  CertExpiryTimes {
        val dataWithError = goBackend.certExpiryTimes
        if (dataWithError.isError) {
            throw CommonException(dataWithError.error)
        }
        if (dataWithError.data.isNullOrEmpty()) {
            throw CommonException(ERROR_EMPTY_RESPONSE)
        }
        return serializerService.deserializeCertExpiryTimes(dataWithError.data!!)
    }

    @kotlin.jvm.Throws(CommonException::class)
    fun removeServer(instance: Instance) {
        val error = goBackend.removeServer(
            instance.authorizationType.toNativeServerType().nativeValue,
            instance.baseURI
        )
        preferencesService.setToken(instance.baseURI, null)
        if (!error.isNullOrEmpty()) {
            throw CommonException(error)
        }
    }

    private fun AuthorizationType.toNativeServerType(): ServerType {
        return when (this) {
            AuthorizationType.Distributed -> ServerType.SecureInternet
            AuthorizationType.Organization -> ServerType.Custom
            AuthorizationType.Local -> ServerType.InstituteAccess
        }
    }

    @kotlin.jvm.Throws(CommonException::class)
    suspend fun handleRedirection(redirectUri: Uri?): Boolean {
        val cookie = pendingOAuthCookie
        val urlString = redirectUri?.toString()
        if (cookie == null || redirectUri == null || urlString.isNullOrEmpty()) {
            return false
        }
        val error = goBackend.cookieReply(cookie, urlString)
        pendingOAuthCookie = null
        if (!error.isNullOrEmpty()) {
            throw CommonException(error)
        }
        return true
    }

    @kotlin.jvm.Throws(CommonException::class, UnknownFormatException::class)
    fun getAddedServers(): AddedServers {
        val dataErrorTuple = goBackend.addedServers
        if (dataErrorTuple.isError) {
            throw CommonException(dataErrorTuple.error)
        }
        return serializerService.deserializeAddedServers(dataErrorTuple.data)
    }

    @kotlin.jvm.Throws(CommonException::class, UnknownFormatException::class)
    suspend fun getConfig(instance: Instance, forceTCP: Boolean) = withContext(Dispatchers.IO) {
        val dataErrorTuple = goBackend.getProfiles(
            instance.authorizationType.toNativeServerType().nativeValue,
            instance.baseURI,
            forceTCP,
            false
        )

        if (dataErrorTuple.isError) {
            throw CommonException(dataErrorTuple.error)
        }
        val config = serializerService.deserializeSerializedVpnConfig(dataErrorTuple.data)
        onConfigReady?.invoke(config, forceTCP)
    }

    @kotlin.jvm.Throws(CommonException::class)
    suspend fun selectProfile(profile: Profile, preferTcp: Boolean) {
        lastSelectedProfile = profile.profileId
        val cookie = pendingProfileSelectionCookie
        if (cookie != null) {
            val result = goBackend.selectProfile(cookie, profile.profileId)
            if (result != null) {
                throw CommonException(result)
            }
            pendingProfileSelectionCookie = null
        } else {
            val result = goBackend.switchProfile(profile.profileId)
            if (result != null) {
                throw CommonException(result)
            }
            val instance = getCurrentServer()?.asInstance()
                ?: throw CommonException("Current server should not be null when switching profiles!")
            getConfig(instance, preferTcp)
        }
    }

    fun selectCountry(cookie: Int?, organizationId: String, countryCode: String) {
        val errorString = if (cookie != null) {
            goBackend.cookieReply(cookie, countryCode)
        } else {
            goBackend.selectCountry(organizationId, countryCode)
        }
        if (errorString != null) {
            throw CommonException(errorString)
        }
    }

    fun getCurrentServer(): CurrentServer? {
        val dataErrorTuple = goBackend.currentServer
        if (dataErrorTuple.isError) {
            Log.e(TAG, "Unable to determine current server!", CommonException(dataErrorTuple.error))
            return null
        }
        return if (dataErrorTuple.data.isNullOrEmpty()) {
            null
        } else {
            serializerService.deserializeCurrentServer(dataErrorTuple.data!!)
        }
    }

    fun cancelPendingRedirect() {
        pendingOAuthCookie?.let {
            goBackend.cancelCookie(it)
            pendingOAuthCookie = null
        }
    }

    fun getLogFile() : File? {
        val configDirectory = File(context.cacheDir, DIRECTORY_BACKEND_CONFIG_FILES)
        val configFile = File(configDirectory, "log")
        if (configFile.exists()) {
            return configFile
        }
        return null
    }

    /**
     * Starts checking if there's a stable connection on the tunnel. Only works on WireGuard for now.
     */
    suspend fun startFailOver(service: VPNService, onFailOverNeeded: () -> Unit) {
        goBackend.updateRxBytesRead(0)
        val updateBytesJob = GlobalScope.launch {
            service.byteCountFlow.collectLatest {
                goBackend.updateRxBytesRead(it?.bytesIn ?: 0L)
            }
        }
        service.ipFlow.collectLatest { ips ->
            val tunnelIp = ips?.tunnelData?.tunnelIp
            var mtu = ips?.tunnelData?.mtu
            if (tunnelIp == null) {
                throw CommonException("Could not start failover, because IP was missing!")
            }
            if (mtu == null) {
                // We could try to get it from the actual network interface
                try {
                    val tunnelInterface = NetworkInterface.getNetworkInterfaces().toList().firstOrNull {
                        it.inetAddresses.toList().any {
                            if (it.hostAddress != null) {
                                it.hostAddress!! in (ips.clientIpv4?.split(",") ?: emptyList()) ||
                                        it.hostAddress!! in (ips.clientIpv6?.split(",") ?: emptyList())
                            } else {
                                false
                            }
                        }
                    }
                    mtu = tunnelInterface?.mtu
                } catch (ex: Exception) {
                    Log.e(TAG, "Could not determine MTU!", ex)
                }
            }
            if (mtu == null) {
                throw CommonException("Could not start failover, because MTU was missing!")
            }
            Log.v(TAG, "Failover started with tunnel IP: $tunnelIp and MTU: $mtu")
            val result = goBackend.startFailOver(tunnelIp, mtu)
            Log.v(TAG, "Failover ended with result: ${result.doesRequireFailover}")
            updateBytesJob.cancel()
            if (result.isError) {
                throw CommonException(result.error)
            }
            if (result.doesRequireFailover) {
                onFailOverNeeded()
            }
        }
    }
}

