package nl.eduvpn.app;

import android.app.Application;
import android.content.Context;

import nl.eduvpn.app.inject.EduVPNComponent;

import de.blinkt.openvpn.core.PRNGFixes;
import de.blinkt.openvpn.core.VpnStatus;

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
