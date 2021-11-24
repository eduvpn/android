package nl.eduvpn.app.wireguard

import android.app.Activity
import android.content.Context
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.eduvpn.app.service.VPNService
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.WireGuardTunnel

/**
 * Service responsible for managing the WireGuard profiles and the connection.
 */
class WireGuardService(private val context: Context) : VPNService() {

    private val backend = GoBackend(context)

    private var errorString: String? = null

    // Stores the current VPN status.
    private var connectionStatus = VPNStatus.DISCONNECTED

    private val TAG = this::class.java.name

    private val tunnel = WireGuardTunnel("eduVPN WireGuard tunnel") { newTunnelState ->
        setConnectionStatus(tunnelStateToStatus(newTunnelState))
    }

    override fun getStatus(): VPNStatus {
        return connectionStatus
    }

    private fun setConnectionStatus(status: VPNStatus) {
        connectionStatus = status
        postValue(status)
    }

    /**
     * Ask user for VPN permission
     */
    private fun authorizeVPN(activity: Activity) {
        val intent = GoBackend.VpnService.prepare(context)
        if (intent != null) {
            //todo: do not ignore result
            activity.startActivityForResult(intent, 0)
        }
    }

    /**
     * Connects to the VPN using the config supplied as a parameter.
     *
     * @param activity   The current activity, required for providing a context.
     * @param config  The config to use for connecting.
     */
    suspend fun connect(activity: Activity, config: Config) {
        setConnectionStatus(VPNStatus.CONNECTING)

        //todo: update ips
        withContext(Dispatchers.Main) {
            authorizeVPN(activity)
        }

        withContext(Dispatchers.IO) {
            try {
                backend.setState(tunnel, Tunnel.State.UP, config)
            } catch (ex: BackendException) {
                if (ex.reason == BackendException.Reason.VPN_NOT_AUTHORIZED) { //todo
                    setConnectionStatus(VPNStatus.DISCONNECTED)
                } else {
                    fail(ex.toString())
                }
            }
        }
    }

    private fun fail(errorString: String) {
        setConnectionStatus(VPNStatus.FAILED)
        this.errorString = errorString
    }

    override fun disconnect() {
        try {
            backend.setState(tunnel, Tunnel.State.DOWN, null)
        } catch (ex: Exception) {
            Log.e(
                TAG,
                "Exception when trying to stop WireGuard connection. Connection might not be closed!",
                ex
            )
        }
    }

    override fun getErrorString(): String? {
        return errorString
    }

    private fun tunnelStateToStatus(tunnelState: Tunnel.State): VPNStatus {
        return when (tunnelState) {
            Tunnel.State.UP -> VPNStatus.CONNECTED
            Tunnel.State.DOWN -> VPNStatus.DISCONNECTED
            Tunnel.State.TOGGLE -> tunnelStateToStatus(tunnelState)
        }
    }

    override fun getProtocolName(): String {
        return "WireGuard"
    }
}
