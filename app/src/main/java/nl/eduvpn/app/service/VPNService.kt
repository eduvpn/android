package nl.eduvpn.app.service

import android.app.Notification
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import nl.eduvpn.app.livedata.ByteCount
import nl.eduvpn.app.livedata.IPs

abstract class VPNService : LiveData<VPNService.VPNStatus>() {

    enum class VPNStatus {
        DISCONNECTED, CONNECTING, CONNECTED, PAUSED, FAILED
    }

    abstract val byteCountFlow: Flow<ByteCount?>

    abstract val ipFlow: Flow<IPs?>

    /**
     *  User should call this after showing a notification.
     *
     *  @param id The identifier for this notification as per
     * {@link NotificationManager#notify(int, Notification)
     * NotificationManager.notify(int, Notification)};
     * @param notification The Notification to be displayed.
     */
    abstract fun startForeground(id: Int, notification: Notification)

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
     * Name of the VPN protocol.
     */
    abstract fun getProtocolName(): String
}
