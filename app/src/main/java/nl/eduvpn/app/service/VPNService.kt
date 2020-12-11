package nl.eduvpn.app.service

import android.app.Activity
import nl.eduvpn.app.entity.VpnConfig
import java.util.*

abstract class VPNService : Observable() {

    enum class VPNStatus {
        DISCONNECTED, CONNECTING, CONNECTED, PAUSED, FAILED
    }

    /**
     * Connects to the VPN using the profile supplied as a parameter.
     *
     * @param activity   The current activity, required for providing a context.
     * @param vpnConfig  The config to use for connecting.
     */
    abstract fun connect(activity: Activity, vpnConfig: VpnConfig)

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

    abstract fun getAbout(): String
}
