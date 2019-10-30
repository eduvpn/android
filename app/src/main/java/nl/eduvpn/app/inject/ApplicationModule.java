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

import android.content.Context;
import android.content.SharedPreferences;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import nl.eduvpn.app.EduVPNApplication;
import nl.eduvpn.app.service.APIService;
import nl.eduvpn.app.service.ConfigurationService;
import nl.eduvpn.app.service.ConnectionService;
import nl.eduvpn.app.service.HistoryService;
import nl.eduvpn.app.service.PreferencesService;
import nl.eduvpn.app.service.SecurityService;
import nl.eduvpn.app.service.SerializerService;
import nl.eduvpn.app.service.VPNService;
import nl.eduvpn.app.utils.Log;
import okhttp3.OkHttpClient;

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
    protected ConfigurationService provideConfigurationService(PreferencesService preferencesService, SerializerService serializerService,
                                                               SecurityService securityService, OkHttpClient okHttpClient) {
        return new ConfigurationService(preferencesService, serializerService, securityService, okHttpClient);
    }

    @Provides
    @Singleton
    protected SharedPreferences provideSecurePreferences(SecurityService securityService) {
        return securityService.getSecurePreferences();
    }

    @Provides
    @Singleton
    protected PreferencesService providePreferencesService(SerializerService serializerService, SharedPreferences sharedPreferences) {
        return new PreferencesService(serializerService, sharedPreferences);
    }

    @Provides
    @Singleton
    protected ConnectionService provideConnectionService(PreferencesService preferencesService, HistoryService historyService,
                                                         SecurityService securityService) {
        return new ConnectionService(preferencesService, historyService, securityService);
    }

    @Provides
    @Singleton
    protected APIService provideAPIService(ConnectionService connectionService, OkHttpClient okHttpClient) {
        return new APIService(connectionService, okHttpClient);
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

    @Provides
    @Singleton
    protected SecurityService provideSecurityService(Context context) {
        return new SecurityService(context);
    }

    @Provides
    @Singleton
    protected OkHttpClient provideHttpClient() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    try {
                        return chain.proceed(chain.request());
                    } catch (ConnectException | SocketTimeoutException | UnknownHostException ex) {
                        Log.d("OkHTTP", "Retrying request because previous one failed with connection exception...");
                        // Wait 3 seconds
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            // Do nothing
                        }
                        return chain.proceed(chain.request());
                    }
                });
        return clientBuilder.build();
    }
}
