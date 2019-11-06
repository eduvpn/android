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

package nl.eduvpn.app.inject;

import org.jetbrains.annotations.NotNull;

import nl.eduvpn.app.EduVPNApplication;
import nl.eduvpn.app.MainActivity;
import nl.eduvpn.app.fragment.ConnectionStatusFragment;
import nl.eduvpn.app.fragment.CustomProviderFragment;
import nl.eduvpn.app.fragment.ProfileSelectionFragment;
import nl.eduvpn.app.fragment.ProviderSelectionFragment;
import nl.eduvpn.app.fragment.ServerSelectionFragment;
import nl.eduvpn.app.fragment.SettingsFragment;

import javax.inject.Singleton;

import dagger.Component;

/**
 * The Dagger component which executes the injections.
 * Created by Daniel Zolnai on 2016-10-07.
 */
@Singleton
@Component(modules = { ApplicationModule.class })
public interface EduVPNComponent {

    final class Initializer {
        public static EduVPNComponent init(EduVPNApplication application) {
            // Don't worry if you see an error here, DaggerEduVPNComponent is generated while building.
            return DaggerEduVPNComponent.builder().applicationModule(new ApplicationModule(application)).build();
        }

        private Initializer() {
            // No instances.
        }
    }

    void inject(ProviderSelectionFragment providerSelectionFragment);

    void inject(MainActivity mainActivity);

    void inject(CustomProviderFragment customProviderFragment);

    void inject(ConnectionStatusFragment connectionStatusFragment);

    void inject(ProfileSelectionFragment homeFragment);

    void inject(SettingsFragment settingsFragment);

    void inject(@NotNull ServerSelectionFragment serverSelectionFragment);
}
