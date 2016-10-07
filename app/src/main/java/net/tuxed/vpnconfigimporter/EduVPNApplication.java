package net.tuxed.vpnconfigimporter;

import android.app.Application;
import android.content.Context;

import net.tuxed.vpnconfigimporter.inject.EduVPNComponent;

import de.blinkt.openvpn.core.PRNGFixes;
import de.blinkt.openvpn.core.VpnStatus;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Application object which keeps track of the app lifecycle.
 * Created by Daniel Zolnai on 2016-09-14.
 */
public class EduVPNApplication extends Application {

    private EduVPNComponent _component;

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
        // Set up the injector
        _component = EduVPNComponent.Initializer.init(this);
    }

    public EduVPNComponent component() {
        return _component;
    }

    public static EduVPNApplication get(Context context) {
        return (EduVPNApplication)context.getApplicationContext();
    }
}
