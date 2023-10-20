package nl.eduvpn.app.service

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nl.eduvpn.app.Constants
import nl.eduvpn.app.DisconnectVPNBroadcastReceiver
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.VPNConfig
import nl.eduvpn.app.utils.FormattingUtils
import nl.eduvpn.app.utils.pendingIntentImmutableFlag

class VPNConnectionService(
    private val preferencesService: PreferencesService,
    private val eduVPNOpenVPNService: EduVPNOpenVPNService,
    private val wireGuardService: WireGuardService,
    private val applicationContext: Context
) {
    private val notificationID = Constants.VPN_CONNECTION_NOTIFICATION_ID

    private var statusObserver: Observer<VPNService.VPNStatus>? = null

    fun disconnect(context: Context, vpnService: VPNService) {
        vpnService.disconnect()
        removeVPNNotification(context, vpnService)
    }

    fun connectionToConfig(
        scope: CoroutineScope,
        activity: Activity,
        vpnConfig: VPNConfig
    ): VPNService {
        val vpnService = when (vpnConfig) {
            is VPNConfig.OpenVPN -> {
                eduVPNOpenVPNService.connect(activity, vpnConfig.profile)
                eduVPNOpenVPNService
            }
            is VPNConfig.WireGuard -> {
                scope.launch {
                    wireGuardService.connect(activity, vpnConfig.config)
                }
                wireGuardService
            }
        }
        val observer: Observer<VPNService.VPNStatus> = Observer { vpnStatus ->
            showVPNNotification(applicationContext, vpnService, vpnStatus)
        }
        vpnService.observeForever(observer)
        observer.onChanged(vpnService.getStatus())
        statusObserver = observer
        return vpnService
    }


    private fun showVPNNotification(
        context: Context,
        vpnService: VPNService,
        vpnStatus: VPNService.VPNStatus
    ) {
        val configName = FormattingUtils.formatProfileName(
            context,
            preferencesService.getCurrentInstance()!!,
            null
        )
        val channelID = Constants.VPN_CONNECTION_NOTIFICATION_CHANNEL_ID

        val disconnectVPNIntent = Intent(context, DisconnectVPNBroadcastReceiver::class.java)
            .setAction(DisconnectVPNBroadcastReceiver.ACTION)
        val disconnectVPNPendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                disconnectVPNIntent,
                pendingIntentImmutableFlag
            )

        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.logo_black)
            .setUsesChronometer(true)
            .setContentTitle(configName)
            .setContentText(context.getString(vpnStatusToStringID(vpnStatus)))
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    pendingIntentImmutableFlag
                )
            )
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Only used on Android <= 7.1
            .addAction(
                de.blinkt.openvpn.R.drawable.ic_menu_close_clear_cancel,
                context.getString(de.blinkt.openvpn.R.string.cancel_connection), disconnectVPNPendingIntent
            )
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Prevent recreating the notification in case we just removed it
        notificationManager.notify(notificationID, notification)
        vpnService.startForeground(notificationID, notification)
    }

    private fun removeVPNNotification(context: Context, vpnService: VPNService) {
        statusObserver?.let { observer -> vpnService.removeObserver(observer) }
        statusObserver = null
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationID)
    }

    companion object {
        fun vpnStatusToStringID(vpnStatus: VPNService.VPNStatus): Int {
            return when (vpnStatus) {
                VPNService.VPNStatus.CONNECTED -> R.string.connection_info_state_connected
                VPNService.VPNStatus.CONNECTING -> R.string.connection_info_state_connecting
                VPNService.VPNStatus.PAUSED -> R.string.connection_info_state_paused
                VPNService.VPNStatus.DISCONNECTED -> R.string.connection_info_state_disconnected
                VPNService.VPNStatus.FAILED -> R.string.connection_info_state_disconnected
            }
        }
    }
}
