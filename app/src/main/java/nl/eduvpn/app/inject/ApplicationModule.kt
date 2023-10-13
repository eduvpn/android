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
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.delay
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.livedata.ConnectionTimeLiveData
import nl.eduvpn.app.livedata.openvpn.IPLiveData
import nl.eduvpn.app.service.*
import nl.eduvpn.app.utils.Log
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Provider
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
        serializerService: SerializerService,
        backendService: BackendService
    ): OrganizationService {
        return OrganizationService(serializerService, backendService)
    }


    @Provides
    @Singleton
    fun provideBackendService(
        context: Context,
        serializerService: SerializerService,
        preferencesService: PreferencesService
    ) : BackendService {
        return BackendService(
            context,
            serializerService,
            preferencesService
        )
    }

    @Provides
    @Singleton
    fun providePreferencesService(
        context: Context,
        serializerService: SerializerService,
    ): PreferencesService {
        return PreferencesService(context, serializerService)
    }

    @Provides
    @Singleton
    fun provideSerializerService(): SerializerService {
        return SerializerService()
    }

    @Provides
    @Singleton
    @Named("timer")
    fun provide1SecondTimer(): LiveData<Unit> {
        return liveData {
            while (true) {
                emit(Unit)
                delay(1000)
            }
        }
    }

    @Provides
    @Named("connectionTimeLiveData")
    fun provideConnectionTimeLiveData(
        vpnService: VPNService,
        @Named("timer") timer: LiveData<Unit>
    ): LiveData<Long?> {
        return ConnectionTimeLiveData.create(vpnService, timer)
    }

    @Provides
    @Singleton
    fun provideOpenVPNIPLiveData(): IPLiveData {
        return IPLiveData()
    }

    @Provides
    @Singleton
    fun provideEduOpenVPNService(
        context: Context,
        preferencesService: PreferencesService?,
        ipLiveData: IPLiveData,
    ): EduVPNOpenVPNService {
        return EduVPNOpenVPNService(context, preferencesService, ipLiveData)
    }

    @Provides
    @Singleton
    fun provideWireGuardService(
        context: Context,
        @Named("timer") timer: LiveData<Unit>
    ): WireGuardService {
        return WireGuardService(context, timer.asFlow())
    }

    @Provides
    fun provideOptionalVPNService(
        preferencesService: PreferencesService,
        eduOpenVPNServiceProvider: Provider<EduVPNOpenVPNService>,
        wireGuardServiceProvider: Provider<WireGuardService>
    ): Optional<VPNService> {
        return when (preferencesService.getCurrentProtocol()) {
            org.eduvpn.common.Protocol.OpenVPN.nativeValue -> Optional.of(eduOpenVPNServiceProvider.get())
            org.eduvpn.common.Protocol.WireGuard.nativeValue -> Optional.of(wireGuardServiceProvider.get())
            else -> Optional.empty()
        }
    }

    @Provides
    fun provideVPNService(optionalVPNService: Optional<VPNService>): VPNService {
        return optionalVPNService.orElseGet {
            throw IllegalStateException("Could not determine what VPNService to use")
        }
    }

    @Provides
    @Singleton
    fun provideHistoryService(
        backendService: BackendService
    ): HistoryService {
        return HistoryService(backendService)
    }

    @Provides
    @Singleton
    fun provideVPNConnectionService(
        preferencesService: PreferencesService,
        eduVPNOpenVPNService: EduVPNOpenVPNService,
        wireGuardService: WireGuardService,
        applicationContext: Context,
    ): VPNConnectionService {
        return VPNConnectionService(
            preferencesService,
            eduVPNOpenVPNService,
            wireGuardService,
            applicationContext,
        )
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
            .let { builder ->
                // Unencrypted traffic is disallowed on Android >= 6, so disallowing redirects from
                // HTTPS to HTTP only applies to Android 5.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    builder.addInterceptor { chain: Interceptor.Chain ->
                        val request = chain.request()
                        val response = chain.proceed(request)
                        if (request.isHttps && response.isRedirect && !response.request.isHttps) {
                            throw IOException("Got redirected from HTTPS to non HTTPS url. Redirected from ${request.url} to ${response.request.url}")
                        }
                        response
                    } else {
                    builder
                }
            }
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
        return clientBuilder.build()
    }
}
