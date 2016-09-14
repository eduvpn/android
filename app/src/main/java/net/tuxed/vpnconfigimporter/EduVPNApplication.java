package net.tuxed.vpnconfigimporter;

import android.app.Application;

import de.blinkt.openvpn.*;
import de.blinkt.openvpn.core.PRNGFixes;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Created by Dani on 2016-09-14.
 */
public class EduVPNApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();
        VpnStatus.initLogCache(getApplicationContext().getCacheDir());
    }
}
