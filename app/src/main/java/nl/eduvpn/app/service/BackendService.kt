package nl.eduvpn.app.service

import android.content.Context
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.utils.Log
import org.eduvpn.common.GoBackend
import org.eduvpn.common.StateCB
import java.io.File

class BackendService(private val context: Context) {

    companion object {
        private const val DIRECTORY_BACKEND_CONFIG_FILES = "backend_config_files"
        private val TAG = BackendService::class.java.simpleName
    }

    private val goBackend = GoBackend()

    fun register() : String? {
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
        return goBackend.register(BuildConfig.OAUTH_CLIENT_ID, version, configFilesDir, 1)
    }
}