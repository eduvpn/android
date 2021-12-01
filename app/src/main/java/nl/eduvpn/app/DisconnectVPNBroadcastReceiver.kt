package nl.eduvpn.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.eduvpn.app.service.VPNConnectionService
import nl.eduvpn.app.service.VPNService
import javax.inject.Inject

class DisconnectVPNBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var vpnConnectionService: VPNConnectionService

    @Inject
    lateinit var vpnService: VPNService

    companion object {
        val ACTION = this::class.qualifiedName!!.plus(".disconnect_vpn")
    }

    override fun onReceive(context: Context, intent: Intent) {
        EduVPNApplication.get(context).component().inject(this)
        if (intent.action != ACTION) {
            return
        }
        vpnConnectionService.disconnect(context, vpnService)
    }

}
