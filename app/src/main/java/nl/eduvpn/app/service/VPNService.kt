package nl.eduvpn.app.service

import java.util.*

abstract class VPNService : Observable() {

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

    /**
     * Attaches a connection info listener callback, which will be called frequently with the latest data.
     *
     * @param callback The callback.
     */
    abstract fun attachConnectionInfoListener(callback: ConnectionInfoCallback?)

    /**
     * Detaches the current connection info listener.
     */
    abstract fun detachConnectionInfoListener()

    interface ConnectionInfoCallback {

        fun updateStatus(secondsConnected: Long?, bytesIn: Long?, bytesOut: Long?)

        fun metadataAvailable(localIpV4: String?, localIpV6: String?)

    }

    abstract fun getProtocolName(): String
}
