package net.tuxed.vpnconfigimporter.inject;

import android.content.Context;

import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.service.APIService;
import net.tuxed.vpnconfigimporter.service.ConfigurationService;
import net.tuxed.vpnconfigimporter.service.ConnectionService;
import net.tuxed.vpnconfigimporter.service.PreferencesService;
import net.tuxed.vpnconfigimporter.service.SerializerService;
import net.tuxed.vpnconfigimporter.service.VPNService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Application module providing the different dependencies
 * Created by Daniel Zolnai on 2016-10-07.
 */
@Module
public class ApplicationModule {

    private final EduVPNApplication _application;

    public ApplicationModule(EduVPNApplication application) {
        _application = application;
    }

    @Provides
    @Singleton
    protected Context provideApplicationContext() {
        return _application.getApplicationContext();
    }

    @Provides
    @Singleton
    protected ConfigurationService provideConfigurationService(Context context, SerializerService serializerService) {
        return new ConfigurationService(context, serializerService);
    }

    @Provides
    @Singleton
    protected PreferencesService providePreferencesService(Context context, SerializerService serializerService) {
        return new PreferencesService(context, serializerService);
    }

    @Provides
    @Singleton
    protected ConnectionService provideConnectionService(Context context, PreferencesService preferencesService) {
        return new ConnectionService(context, preferencesService);
    }

    @Provides
    @Singleton
    protected APIService provideAPIService(ConnectionService connectionService) {
        return new APIService(connectionService);
    }

    @Provides
    @Singleton
    protected SerializerService provideSerializerService() {
        return new SerializerService();
    }

    @Provides
    @Singleton
    protected VPNService provideVPNService(Context context) {
        return new VPNService(context);
    }
}
