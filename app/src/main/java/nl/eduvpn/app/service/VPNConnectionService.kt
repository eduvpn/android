package nl.eduvpn.app.service

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.eduvpn.app.Constants
import nl.eduvpn.app.DisconnectVPNBroadcastReceiver
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.entity.TranslatableString
import nl.eduvpn.app.entity.VPNConfig
import nl.eduvpn.app.utils.FormattingUtils
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.pendingIntentImmutableFlag
import nl.eduvpn.app.utils.runCatchingCoroutine

class VPNConnectionService(
    private val preferencesService: PreferencesService,
    private val historyService: HistoryService,
    private val apiService: APIService,
    private val eduVPNOpenVPNService: EduVPNOpenVPNService,
    private val wireGuardService: WireGuardService,
    private val applicationContext: Context
) {

    private val TAG = this::class.qualifiedName

    private val notificationID = Constants.VPN_CONNECTION_NOTIFICATION_ID

    private var statusObserver: Observer<VPNService.VPNStatus>? = null

    fun disconnect(context: Context, vpnService: VPNService) {
        vpnService.disconnect()
        removeVPNNotification(context, vpnService)
        disconnectCall()
    }

    @OptIn(DelicateCoroutinesApi::class) // Necessary for GlobalScope.launch
    private fun disconnectCall() {
        val discoveredAPI = preferencesService.getCurrentDiscoveredAPI()
        if (discoveredAPI == null) {
            Log.e(TAG, "No discovered API available when trying to disconnect.")
            return
        }
        val instance = preferencesService.getCurrentInstance()
        if (instance == null) {
            Log.e(TAG, "No instance available when trying to disconnect.")
            return
        }
        // We do not wait for the disconnect request to finish when disconnecting,
        // but when connecting again, a call to the API will wait for the disconnect to finish.
        GlobalScope.launch {
            runCatchingCoroutine {
                throw RuntimeException("Should be removed!")
            }.onSuccess {
                Log.d(TAG, "Successfully send disconnect to server.")
            }.onFailure { thr ->
                Log.d(TAG, "Failed sending disconnect to server: $thr")
            }
        }
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

            Profile("TODO", TranslatableString(), null)// preferencesService.getCurrentProfile()!!
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
                R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.cancel_connection), disconnectVPNPendingIntent
            )
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Prevent recreating the notification in case we just removed it
        if(statusObserver != null) {
            notificationManager.notify(notificationID, notification)

            vpnService.startForeground(notificationID, notification)
        }
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
