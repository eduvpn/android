package nl.eduvpn.app.service

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.utils.Log
import org.eduvpn.common.CommonException
import org.eduvpn.common.GoBackend
import org.eduvpn.common.GoBackend.Callback
import org.eduvpn.common.ServerType
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BackendService(
    private val context: Context,
    private val serializerService: SerializerService
) {

    companion object {
        private const val DIRECTORY_BACKEND_CONFIG_FILES = "backend_config_files"
        private const val ERROR_EMPTY_RESPONSE = "Empty response returned by common module"

        private val TAG = BackendService::class.java.simpleName

        const val NATIVE_CALL_TIMEOUT_MILLISECONDS = 10_000L
    }

    enum class State(val nativeValue: Int) {
        INITIAL(1),
        OAUTH_STARTED(6)
    }

    private val goBackend = GoBackend()

    private val internalStateListeners = mutableListOf<Callback>()

    private var pendingOAuthCookie: Int? = null

    fun register(): String? {
        GoBackend.callbackFunction = Callback { newState, data ->
            internalStateListeners.forEach { it.onNewState(newState, data) }
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

        // TODO do we need statecb?
        return goBackend.register(
            BuildConfig.OAUTH_CLIENT_ID,
            version,
            configFilesDir,
            BuildConfig.DEBUG
        )
    }

    fun deregister() {
        GoBackend.callbackFunction = null
        internalStateListeners.clear()
        // TODO call native deregister
    }

    @Throws(CommonException::class)
    fun discoverOrganizations() : String {
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
    fun discoverServers() : String {
        val dataWithError = goBackend.discoverServers()
        if (dataWithError.isError) {
            throw CommonException(dataWithError.error)
        }
        if (dataWithError.data.isNullOrEmpty()) {
            throw CommonException(ERROR_EMPTY_RESPONSE)
        }
        return dataWithError.data!!
    }

    @kotlin.jvm.Throws(TimeoutCancellationException::class, CommonException::class)
    suspend fun addServer(instance: Instance) : String {
        val jsonString = suspendCoroutineWithTimeout<String> { continuation ->
            internalStateListeners.add(object : Callback {
                override fun onNewState(newState: Int, data: String?) {
                    if (newState == State.OAUTH_STARTED.nativeValue) {
                        internalStateListeners.remove(this)
                        if (data.isNullOrEmpty()) {
                            continuation.resumeWithException(CommonException(ERROR_EMPTY_RESPONSE))
                            return
                        }
                        continuation.resume(data)
                    } else if (newState == State.INITIAL.nativeValue) {
                        internalStateListeners.remove(this)
                        continuation.resumeWithException(CommonException("Could not connect to instance."))
                        return
                    }
                }
            })
            goBackend.addServer(
                instance.authorizationType.toNativeServerType().nativeValue,
                instance.baseURI
            )
        }
        // Parse the JSON
        val resultObject = serializerService.deserializeCookieAndData(jsonString)
        pendingOAuthCookie = resultObject.cookie
        return resultObject.data
    }

    private fun AuthorizationType.toNativeServerType(): ServerType {
        return when (this) {
            AuthorizationType.Distributed ->  ServerType.SecureInternet
            AuthorizationType.Local -> ServerType.Custom
            AuthorizationType.Organization -> ServerType.InstituteAccess
        }
    }

    suspend inline fun <T> suspendCoroutineWithTimeout(
        crossinline block: (Continuation<T>) -> Unit
    ) = withTimeout(NATIVE_CALL_TIMEOUT_MILLISECONDS) {
        suspendCancellableCoroutine(block = block)
    }

    @kotlin.jvm.Throws(CommonException::class)
    fun handleRedirection(redirectUri: Uri?) : Boolean {
        val cookie = pendingOAuthCookie
        val urlString = redirectUri?.toString()
        if (cookie == null || redirectUri == null || urlString.isNullOrEmpty()) {
            return false
        }
        System.out.println("Result from redirection: " + goBackend.handleRedirection(cookie, urlString));
        return true
    }
}

