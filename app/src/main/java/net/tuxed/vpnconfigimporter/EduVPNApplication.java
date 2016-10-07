package net.tuxed.vpnconfigimporter;

import android.app.Application;

import de.blinkt.openvpn.*;
import de.blinkt.openvpn.core.PRNGFixes;
import de.blinkt.openvpn.core.VpnStatus;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Application object which keeps track of the app lifecycle.
 * Created by Daniel Zolnai on 2016-09-14.
 */
public class EduVPNApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // These are required by the VPN library
        PRNGFixes.apply();
        VpnStatus.initLogCache(getApplicationContext().getCacheDir());

        // This is required by Calligraphy for nicer fonts
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());
    }
}
