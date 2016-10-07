package net.tuxed.vpnconfigimporter.inject;

import net.tuxed.vpnconfigimporter.fragment.ProviderSelectionFragment;

import dagger.Component;

/**
 * The Dagger component which executes the injections.
 * Created by Daniel Zolnai on 2016-10-07.
 */
@Component(modules = { ApplicationModule.class })
public interface EduVPNComponent {

    final class Initializer {
        public static EduVPNComponent init() {
            // Don't worry if you see an error here, DaggerEduVPNComponent is generated while building.
            return DaggerEduVPNComponent.builder().build();
        }

        private Initializer() {
            // No instances.
        }
    }

    void inject(ProviderSelectionFragment providerSelectionFragment);
}
