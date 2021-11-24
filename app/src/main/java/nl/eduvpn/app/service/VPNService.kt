package nl.eduvpn.app.service

import androidx.lifecycle.LiveData

abstract class VPNService : LiveData<VPNService.VPNStatus>() {

    enum class VPNStatus {
        DISCONNECTED, CONNECTING, CONNECTED, PAUSED, FAILED
    }

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
