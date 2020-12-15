package nl.eduvpn.app.wireguard

import android.app.Activity
import android.content.Context
import android.os.Handler
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.Interface
import kotlinx.coroutines.*
import nl.eduvpn.app.service.VPNService
import nl.eduvpn.app.utils.Log

/**
 * Service responsible for managing the WireGuard profiles and the connection.
 * todo: support always-on vpn
 * todo: support per-app vpn
 * todo: show notification which allows the user to disable the vpn
 */
class WireGuardService(private val context: Context) : VPNService() {

    //todo: support kernel module
    private val backend = GoBackend(context)

    private var config: Config? = null

    // Stores the current VPN status.
    private var connectionStatus = VPNStatus.DISCONNECTED

    // These are used to provide connection info updates
    private var connectionInfoCallback: ConnectionInfoCallback? = null
    private val updatesHandler = Handler()

    // These store the current connection statistics
    private var connectionTime: Long? = null
    private var bytesIn: Long? = null
    private var bytesOut: Long? = null
    private var errorResource: Int? = null

    private var errorString: String? = null

    private val CONNECTION_INFO_UPDATE_INTERVAL_MS = 1000L
    private val TAG = this::class.java.name

    private val tunnel = WireGuardTunnel("eduVPN WireGuard tunnel") { newTunnelState ->
        if (newTunnelState == Tunnel.State.UP) {
            connectionTime = System.currentTimeMillis()
        } else if (newTunnelState == Tunnel.State.DOWN) {
            onDisconnect()
        }
        connectionStatus = tunnelStateToStatus(newTunnelState)
        // Notify the observers.
        updatesHandler.post {
            setChanged()
            notifyObservers(getStatus())
        }
    }

    /**
     * Ask user for VPN permission
     */
    private fun authorizeVPN(activity: Activity) {
        val intent = GoBackend.VpnService.prepare(context)
        if (intent != null) {
            activity.startActivity(intent)
        }
    }

    /**
     * Connects to the VPN using the config supplied as a parameter.
     *
     * @param activity   The current activity, required for providing a context.
     * @param vpnConfig  The config to use for connecting.
     */
    fun connect(activity: Activity, config: Config) {

        connectionStatus = VPNStatus.CONNECTING

        this.config = config

        if (connectionInfoCallback != null) {
            updateIPs(config.`interface`, connectionInfoCallback!!)
        }

        authorizeVPN(activity)

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    backend.setState(tunnel, Tunnel.State.UP, config)
                } catch (ex: BackendException) {
                    if (ex.reason == BackendException.Reason.VPN_NOT_AUTHORIZED) {
                        connectionStatus = VPNStatus.DISCONNECTED
                    } else {
                        fail(ex.toString())
                    }
                }
            }
        }
    }

    private fun fail(errorString: String) {
        connectionStatus = VPNStatus.FAILED
        this.errorString = errorString
    }

    override fun disconnect() {
        try {
            backend.setState(tunnel, Tunnel.State.DOWN, null)
        } catch (ex: Exception) {
            Log.e(TAG, "Exception when trying to stop WireGuard connection. Connection might not be closed!", ex)
        }
        onDisconnect()
    }

    /**
     * Call this if the service has disconnected. Resets all statistics.
     */
    private fun onDisconnect() {
        // Reset all statistics
        connectionTime = null
        errorResource = null
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

    override fun getStatus(): VPNStatus {
        return connectionStatus
    }

    override fun attachConnectionInfoListener(callback: ConnectionInfoCallback?) {
        connectionInfoCallback = callback
        if (callback != null) {
            if (config != null) {
                updateIPs(config!!.`interface`, callback)
            }
            updatesHandler.post(object : Runnable {
                override fun run() {
                    if (connectionInfoCallback != null) {
                        var secondsElapsed: Long? = null
                        if (connectionTime != null) {
                            secondsElapsed = (System.currentTimeMillis() - connectionTime!!) / 1000L
                        }
                        val statistics = backend.getStatistics(tunnel)
                        bytesIn = statistics.totalRx()
                        bytesOut = statistics.totalTx()
                        connectionInfoCallback!!.updateStatus(secondsElapsed, bytesIn, bytesOut)
                        updatesHandler.postDelayed(this, CONNECTION_INFO_UPDATE_INTERVAL_MS)
                    }
                }
            })
        }
    }

    override fun detachConnectionInfoListener() {
        connectionInfoCallback = null
        updatesHandler.removeCallbacksAndMessages(null)
    }

    override fun getProtocolName(): String {
        return "WireGuard"
    }

    companion object {
        private fun updateIPs(wgInterface: Interface, callback: ConnectionInfoCallback) {
            val ipv4Addresses = wgInterface.addresses.filter { network ->
                network.address is java.net.Inet4Address
            }.map { ip ->
                if (ip.mask == 32) {
                    ip.address.hostAddress
                } else {
                    ip.address.getHostAddress() + '/' + ip.mask
                }
            }

            val ipv6Addresses = wgInterface.addresses.filter { network ->
                network.address is java.net.Inet6Address
            }.map { ip ->
                if (ip.mask == 128) {
                    ip.address.hostAddress
                } else {
                    ip.address.getHostAddress() + '/' + ip.mask
                }
            }

            fun ipListToString(ipList: List<String>): String? {
                return ipList.reduceOrNull { s1, s2 -> "$s1, $s2" }
            }
            callback.metadataAvailable(ipListToString(ipv4Addresses), ipListToString(ipv6Addresses))
        }
    }

}
