package net.tuxed.vpnconfigimporter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;

/**
 * Convenience class for handling VPN-related actions.
 * Created by Daniel Zolnai on 2016-09-13.
 */
public class VpnUtils {

    private static final String TAG = VpnUtils.class.getName();

    public static void startConnectionWithProfile(Context context, VpnProfile vpnProfile) {
        Log.i(TAG, String.format("Initiating connection with profile '%s'", vpnProfile.getUUIDString()));
        Intent intent = new Intent(context, LaunchVPN.class);
        intent.putExtra(LaunchVPN.EXTRA_KEY, vpnProfile.getUUIDString());
        intent.setAction(Intent.ACTION_MAIN);
        context.startActivity(intent);
    }
}
