package nl.eduvpn.app.inject;

import android.content.Context;

import nl.eduvpn.app.EduVPNApplication;
import nl.eduvpn.app.service.APIService;
import nl.eduvpn.app.service.ConfigurationService;
import nl.eduvpn.app.service.ConnectionService;
import nl.eduvpn.app.service.HistoryService;
import nl.eduvpn.app.service.PreferencesService;
import nl.eduvpn.app.service.SerializerService;
import nl.eduvpn.app.service.VPNService;

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
    protected ConnectionService provideConnectionService(Context context, PreferencesService preferencesService, HistoryService historyService) {
        return new ConnectionService(context, preferencesService, historyService);
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
    protected VPNService provideVPNService(Context context, PreferencesService preferencesService) {
        return new VPNService(context, preferencesService);
    }

    @Provides
    @Singleton
    protected HistoryService provideHistoryService(PreferencesService preferencesService) {
        return new HistoryService(preferencesService);
    }
}
