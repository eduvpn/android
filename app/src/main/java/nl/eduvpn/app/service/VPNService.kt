package nl.eduvpn.app.service

import androidx.lifecycle.LiveData
import nl.eduvpn.app.livedata.ByteCount
import nl.eduvpn.app.livedata.IPs

abstract class VPNService : LiveData<VPNService.VPNStatus>() {

    enum class VPNStatus {
        DISCONNECTED, CONNECTING, CONNECTED, PAUSED, FAILED
    }

    abstract val byteCountLiveData: LiveData<ByteCount?>

    abstract val ipLiveData: LiveData<IPs>

    /**
     * If the vpn provides it's own notification.
     */
    abstract val showsNotification: Boolean

    /**
     * Disconnects the current VPN connection.
     */
    abstract fun disconnect()

    /**
     * Returns the error string.
     *
     * @return The description of the error.
     */
    abstract fun getErrorString(): String?

    /**
     * @return The current status of the VPN.
     */
    abstract fun getStatus(): VPNStatus

    abstract fun getProtocolName(): String
}
