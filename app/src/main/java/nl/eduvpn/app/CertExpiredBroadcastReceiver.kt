package nl.eduvpn.app

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import nl.eduvpn.app.service.VPNService
import nl.eduvpn.app.utils.pendingIntentImmutableFlag
import javax.inject.Inject

class CertExpiredBroadcastReceiver : BroadcastReceiver() {

    @Inject
    protected lateinit var vpnService: VPNService

    override fun onReceive(context: Context, intent: Intent) {
        EduVPNApplication.get(context).component().inject(this)
        if (intent.action != ACTION) {
            return
        }
        if (vpnService.getStatus() == VPNService.VPNStatus.DISCONNECTED) {
            return
        }
        val channelID = Constants.CERT_EXPIRY_NOTIFICATION_CHANNEL_ID
        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.logo_black)
            .setContentTitle(context.getString(R.string.cert_expiry_notification_title))
            .setContentText(context.getString(R.string.cert_expiry_notification_text))
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    pendingIntentImmutableFlag
                )
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Only used on Android <= 7.1
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(Constants.CERT_EXPIRY_NOTIFICATION_ID, notification)
    }

    companion object {
        val ACTION = this::class.qualifiedName!!.plus(".cert_expiry")
    }
}
