package nl.eduvpn.app.service

import android.app.Activity
import android.app.Notification
import android.content.Context
import android.os.ParcelFileDescriptor
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.Interface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.eduvpn.app.livedata.ByteCount
import nl.eduvpn.app.livedata.IPs
import nl.eduvpn.app.livedata.TunnelData
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.WireGuardTunnel
import org.eduvpn.common.Protocol
import java.lang.reflect.Method
import java.net.Inet4Address
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.jvm.optionals.getOrNull

/**
 * Service responsible for managing the WireGuard profiles and the connection.
 */
@OptIn(DelicateCoroutinesApi::class)
class WireGuardService(private val context: Context, timer: Flow<Unit>): VPNService() {

    private lateinit var backend : GoBackend

    // If we don't run the WireGuard backend always on the same thread, it will crash randomly in native code.
    // So we confine it to the same background thread, and communicate with it via Coroutines.
    private val backendDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

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

    private var pendingConfig: Config? = null

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
        val intent = GoBackend.VpnService.prepare(activity)
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
                        pendingConfig = config
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

    override fun getProtocol(): Protocol {
        return Protocol.WireGuard
    }

    fun tryResumeConnecting(activity: Activity) {
        GlobalScope.launch(backendDispatcher) {
            pendingConfig?.let {
                connect(activity, it)
            }
            pendingConfig = null
        }
    }

    fun protectSocket(fd: Int) {
        // We need to use reflection here, because the required fields are not exposed sadly.
        val field = GoBackend::class.java.getDeclaredField("vpnService")
        // Make the field accessible since it's private
        field.isAccessible = true
        // Get the value of the vpnService field from the backend object
        val vpnServiceFuture = field.get(backend)
        // GhettoCompletableFuture class has a get() method to get the result
        val vpnServiceInstance = vpnServiceFuture.javaClass.getMethod("get", Long::class.java, TimeUnit::class.java)
            .invoke(vpnServiceFuture, 10L, TimeUnit.SECONDS)
        val protectMethod: Method = vpnServiceInstance.javaClass.getMethod("protect", Int::class.java)
        val result = protectMethod.invoke(vpnServiceInstance, fd) as Boolean
        Log.i(TAG, "Protected socket with success: $result")
    }

    companion object {
        private fun calculateTunnelAddress(ipAddress: String?, subnetMask: Int): String? {
            if (ipAddress == null) {
                return null
            }
            try {
                val ipParts = ipAddress.split(".")
                if (ipParts.size != 4) {
                    return null // Invalid IP address format
                }

                val subnetMaskParts = Array(4) { 0 }
                for (i in 0 until subnetMask) {
                    subnetMaskParts[i / 8] = subnetMaskParts[i / 8] or (1 shl (7 - i % 8))
                }

                val networkAddressParts = Array(4) { 0 }
                for (i in 0 until 4) {
                    networkAddressParts[i] = ipParts[i].toInt() and subnetMaskParts[i]
                }
                // Calculate the first valid IP address by adding 1 to the last part of the network address
                networkAddressParts[3]++
                return networkAddressParts.joinToString(".")
            } catch (e: Exception) {
                return null
            }
        }

        private fun getIPs(wgInterface: Interface): IPs {
            val ipv4Addresses = wgInterface.addresses
                .filter { network -> network.address is java.net.Inet4Address }
                .mapNotNull { ip -> ip.address.hostAddress }

            val ipv6Addresses = wgInterface.addresses
                .filter { network -> network.address is java.net.Inet6Address }
                .mapNotNull { ip -> ip.address.hostAddress }

            val tunnelIp = wgInterface.addresses
                .firstOrNull { network -> network.address is Inet4Address }
                ?.let { ip ->
                    calculateTunnelAddress(ip.address.hostAddress, ip.mask)
                }
            fun ipListToString(ipList: List<String>): String? {
                return ipList.reduceOrNull { s1, s2 -> "$s1, $s2" }
            }
            return IPs(
                ipListToString(ipv4Addresses),
                ipListToString(ipv6Addresses),
                TunnelData(tunnelIp, wgInterface.mtu.getOrNull())
            )
        }
    }
}
