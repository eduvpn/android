/*
 *  This file is part of eduVPN.
 *
 *     eduVPN is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     eduVPN is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.eduvpn.app.livedata

import android.content.Intent
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.VpnStatus
import de.blinkt.openvpn.core.VpnStatus.StateListener
import nl.eduvpn.app.service.VPNService
import nl.eduvpn.app.utils.Log
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import java.util.regex.Pattern

class IPLiveData : LiveData<IPLiveData.IPs>() {

    data class IPs(val ipv4: String?, val ipv6: String?)

    private val VPN_INTERFACE_NAME = "tun0"
    private val TAG = VPNService::class.java.name

    private val stateListener: StateListener = object : StateListener {
        override fun updateState(
            state: String?,
            logmessage: String?,
            localizedResId: Int,
            level: ConnectionStatus,
            intent: Intent?
        ) {
            if(VPNService.connectionStatusToVPNStatus(level) == VPNService.VPNStatus.DISCONNECTED) {
                postValue(IPs(null, null))
                return
            }
            // Try to get the address from a lookup
            var ips: Pair<String?, String?>? = lookupVpnIpAddresses()
            if (ips != null) {
                postValue(IPs(ips.first, ips.second))
            } else {
                Log.i(
                    TAG,
                    "Unable to determine IP addresses from network interface lookup, using log message instead."
                )
                ips = logmessage?.let { lm -> parseVpnIpAddressesFromLogMessage(lm) }
                if (ips != null) {
                    postValue(IPs(ips.first, ips.second))
                }
            }
        }

        override fun setConnectedVPN(uuid: String) {}
    }

    override fun onActive() {
        VpnStatus.addStateListener(stateListener)
    }

    override fun onInactive() {
        VpnStatus.removeStateListener(stateListener)
    }

    /**
     * Retrieves the IP4 and IPv6 addresses assigned by the VPN server to this client using a network interface lookup.
     *
     * @return The IPv4 and IPv6 addresses in this order as a pair. If not found, a null value is returned instead.
     */
    private fun lookupVpnIpAddresses(): Pair<String?, String?>? {
        try {
            val networkInterfaces: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in networkInterfaces) {
                if (VPN_INTERFACE_NAME == networkInterface.name) {
                    val addresses: List<InetAddress> =
                        Collections.list(networkInterface.inetAddresses)
                    var ipV4: String? = null
                    var ipV6: String? = null
                    for (address in addresses) {
                        val ip = address.hostAddress
                        val isIPv4 = ip.indexOf(':') < 0
                        if (isIPv4) {
                            ipV4 = ip
                        } else {
                            val delimiter = ip.indexOf('%')
                            ipV6 =
                                if (delimiter < 0) ip.toLowerCase() else ip.substring(0, delimiter)
                                    .toLowerCase()
                        }
                    }
                    return if (ipV4 != null || ipV6 != null) {
                        Pair(ipV4, ipV6)
                    } else {
                        null
                    }
                }
            }
        } catch (ex: SocketException) {
            Log.w(TAG, "Unable to retrieve network interface info!", ex)
        }
        return null
    }

    /**
     * Parses the IPv4 and IPv6 from the log message.
     *
     * @param logMessage The log message to parse from.
     * @return The IPv4 and IPv6 addresses as a pair in this order. If the parsing failed (unexpected format), then a null value will be returned.
     */
    private fun parseVpnIpAddressesFromLogMessage(logMessage: String): Pair<String?, String?>? {
        if (logMessage.isNotEmpty()) {
            val splits = logMessage.split(Pattern.quote(",").toRegex()).toTypedArray()
            if (splits.size == 7) {
                val ipV4 = splits[1].takeUnless { s -> s.isEmpty() }
                val ipV6 = splits[6].takeUnless { s -> s.isEmpty() }
                return Pair(ipV4, ipV6)
            }
        }
        return null
    }
}
