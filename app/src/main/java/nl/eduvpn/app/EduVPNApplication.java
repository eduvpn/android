/*
 *  This file is part of eduVPN.
 *
 *     eduVPN is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     eduVPN is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.eduvpn.app;

import android.content.Context;
import android.os.StrictMode;

import de.blinkt.openvpn.core.ICSOpenVPNApplication;
import nl.eduvpn.app.inject.EduVPNComponent;

/**
 * Application object which keeps track of the app lifecycle.
 * Created by Daniel Zolnai on 2016-09-14.
 */
public class EduVPNApplication extends ICSOpenVPNApplication {

    private EduVPNComponent _component;

    @Override
    public void onCreate() {
        super.onCreate();
        // Set up the injector
        _component = EduVPNComponent.Initializer.init(this);

        // The base class sets a strict VM policy for debug builds, which do not work well with OkHttp
        // (see this issue: https://github.com/square/okhttp/issues/3537)
        // For now, the best solution seems to be disabling strict mode
        StrictMode.VmPolicy policy = new StrictMode.VmPolicy.Builder().build();
        StrictMode.setVmPolicy(policy);
    }

    public EduVPNComponent component() {
        return _component;
    }

    public static EduVPNApplication get(Context context) {
        return (EduVPNApplication)context.getApplicationContext();
    }
}
