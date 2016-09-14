package net.tuxed.vpnconfigimporter;

import android.app.Application;

import de.blinkt.openvpn.*;
import de.blinkt.openvpn.core.PRNGFixes;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Application object which keeps track of the app lifecycle.
 * Created by Daniel Zolnai on 2016-09-14.
 */
public class EduVPNApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();
        VpnStatus.initLogCache(getApplicationContext().getCacheDir());
    }
}
