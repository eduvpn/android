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
package nl.eduvpn.app.inject

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.service.*
import nl.eduvpn.app.utils.Log
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Application module providing the different dependencies
 * Created by Daniel Zolnai on 2016-10-07.
 */
@Module(includes = [ViewModelModule::class])
class ApplicationModule(private val application: EduVPNApplication) {
    @Provides
    @Singleton
    fun provideApplicationContext(): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideOrganizationService(
        serializerService: SerializerService?,
        securityService: SecurityService?, okHttpClient: OkHttpClient?
    ): OrganizationService {
        return OrganizationService(serializerService!!, securityService!!, okHttpClient!!)
    }

    @Provides
    @Singleton
    fun provideSecurePreferences(securityService: SecurityService): SharedPreferences {
        @Suppress("DEPRECATION")
        return securityService.securePreferences
    }

    @Provides
    @Singleton
    fun providePreferencesService(
        context: Context,
        serializerService: SerializerService,
        sharedPreferences: SharedPreferences
    ): PreferencesService {
        return PreferencesService(context, serializerService, sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideConnectionService(
        preferencesService: PreferencesService?, historyService: HistoryService?,
        securityService: SecurityService?
    ): ConnectionService {
        return ConnectionService(preferencesService!!, historyService!!, securityService!!)
    }

    @Provides
    @Singleton
    fun provideAPIService(
        connectionService: ConnectionService?,
        okHttpClient: OkHttpClient?
    ): APIService {
        return APIService(connectionService!!, okHttpClient!!)
    }

    @Provides
    @Singleton
    fun provideSerializerService(): SerializerService {
        return SerializerService()
    }

    @Provides
    @Singleton
    fun provideVPNService(context: Context?, preferencesService: PreferencesService?): VPNService {
        return VPNService(context, preferencesService)
    }

    @Provides
    @Singleton
    fun provideHistoryService(preferencesService: PreferencesService?): HistoryService {
        return HistoryService(preferencesService!!)
    }

    @Provides
    @Singleton
    fun provideSecurityService(context: Context?): SecurityService {
        return SecurityService(context)
    }

    @Provides
    @Singleton
    fun provideHttpClient(context: Context): OkHttpClient {
        val cacheDirectory = context.cacheDir
        val CACHE_SIZE = (16 * 1024 * 1024).toLong() // 16 Mb
        val clientBuilder = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .cache(Cache(cacheDirectory, CACHE_SIZE))
            .followSslRedirects(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain: Interceptor.Chain ->
                try {
                    return@addInterceptor chain.proceed(chain.request())
                } catch (ex: ConnectException) {
                    Log.d(
                        "OkHTTP",
                        "Retrying request because previous one failed with connection exception..."
                    )
                    // Wait 3 seconds
                    try {
                        Thread.sleep(3000)
                    } catch (e: InterruptedException) {
                        // Do nothing
                    }
                    return@addInterceptor chain.proceed(chain.request())
                } catch (ex: SocketTimeoutException) {
                    Log.d(
                        "OkHTTP",
                        "Retrying request because previous one failed with connection exception..."
                    )
                    try {
                        Thread.sleep(3000)
                    } catch (e: InterruptedException) {
                    }
                    return@addInterceptor chain.proceed(chain.request())
                } catch (ex: UnknownHostException) {
                    Log.d(
                        "OkHTTP",
                        "Retrying request because previous one failed with connection exception..."
                    )
                    try {
                        Thread.sleep(3000)
                    } catch (e: InterruptedException) {
                    }
                    return@addInterceptor chain.proceed(chain.request())
                }
            }
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
            clientBuilder.addInterceptor(logging)
        }
        return clientBuilder.build()
    }
}
