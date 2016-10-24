package nl.eduvpn.app.inject;

import nl.eduvpn.app.EduVPNApplication;
import nl.eduvpn.app.MainActivity;
import nl.eduvpn.app.fragment.ConnectionStatusFragment;
import nl.eduvpn.app.fragment.CustomProviderFragment;
import nl.eduvpn.app.fragment.HomeFragment;
import nl.eduvpn.app.fragment.ProviderSelectionFragment;
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

    void inject(HomeFragment homeFragment);

    void inject(SettingsFragment settingsFragment);
}
