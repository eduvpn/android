package net.tuxed.vpnconfigimporter.inject;

import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.MainActivity;
import net.tuxed.vpnconfigimporter.fragment.ConnectionStatusFragment;
import net.tuxed.vpnconfigimporter.fragment.CustomProviderFragment;
import net.tuxed.vpnconfigimporter.fragment.HomeFragment;
import net.tuxed.vpnconfigimporter.fragment.ProviderSelectionFragment;

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
}
