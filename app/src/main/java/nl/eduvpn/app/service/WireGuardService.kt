package nl.eduvpn.app.service

import android.app.Activity
import android.app.Notification
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.Interface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.eduvpn.app.livedata.ByteCount
import nl.eduvpn.app.livedata.IPs
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.WireGuardTunnel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Service responsible for managing the WireGuard profiles and the connection.
 */
class WireGuardService(private val context: Context, timer: Flow<Unit>): VPNService() {

    private lateinit var backend : GoBackend
    private val backendDispatcher = Dispatchers.IO.limitedParallelism(1)

    private var errorString: String? = null

    // Stores the current VPN status.
    private var connectionStatus = VPNStatus.DISCONNECTED

    private val TAG = this::class.java.name

    override val byteCountFlow: Flow<ByteCount?> = timer.map {
        getByteCount()
    }

    private val _ipFlow = MutableStateFlow<IPs?>(null)
    override val ipFlow: Flow<IPs?> = _ipFlow


    private val tunnel = WireGuardTunnel("eduVPN WireGuard tunnel") { newTunnelState ->
        setConnectionStatus(tunnelStateToStatus(newTunnelState))
    }

    init {
        GlobalScope.launch(backendDispatcher) {
            backend = GoBackend(context)
        }
    }

    override fun getStatus(): VPNStatus {
        return connectionStatus
    }

    override fun startForeground(id: Int, notification: Notification) {
        // Not necessary for WireGuard
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
            activity.startActivityForResult(intent, 0)
        }
    }

    private suspend fun getByteCount(): ByteCount? {
        return suspendCoroutine{ continuation ->
            GlobalScope.launch(backendDispatcher) {
                if (!backend.runningTunnelNames.contains(tunnel.name)) {
                    continuation.resume(null)
                    return@launch
                }
                val statistics = backend.getStatistics(tunnel)
                val bytesIn = statistics.totalRx()
                val bytesOut = statistics.totalTx()
                continuation.resume(ByteCount(bytesIn, bytesOut))
            }
        }
    }

    /**
     * Connects to the VPN using the config supplied as a parameter.
     *
     * @param activity   The current activity, required for providing a context.
     * @param config  The config to use for connecting.
     */
    suspend fun connect(activity: Activity, config: Config) {
        withContext(backendDispatcher) {
            setConnectionStatus(VPNStatus.CONNECTING)
            _ipFlow.emit(getIPs(config.`interface`))
            try {
                backend.setState(tunnel, Tunnel.State.UP, config)
            } catch (ex: BackendException) {
                if (ex.reason == BackendException.Reason.VPN_NOT_AUTHORIZED) {
                    withContext(Dispatchers.Main) {
                        authorizeVPN(activity)
                        setConnectionStatus(VPNStatus.DISCONNECTED)
                    }
                } else {
                    fail(ex.toString())
                }
            }
        }
    }

    private fun fail(errorString: String) {
        this.errorString = errorString
        setConnectionStatus(VPNStatus.FAILED)
    }

    override fun disconnect() {
        GlobalScope.launch(backendDispatcher) {
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

    companion object {
        private fun getIPs(wgInterface: Interface): IPs {
            val ipv4Addresses = wgInterface.addresses
                .filter { network -> network.address is java.net.Inet4Address }
                .mapNotNull { ip -> ip.address.hostAddress }

            val ipv6Addresses = wgInterface.addresses
                .filter { network -> network.address is java.net.Inet6Address }
                .mapNotNull { ip -> ip.address.hostAddress }

            fun ipListToString(ipList: List<String>): String? {
                return ipList.reduceOrNull { s1, s2 -> "$s1, $s2" }
            }
            return IPs(ipListToString(ipv4Addresses), ipListToString(ipv6Addresses))
        }
    }
}
