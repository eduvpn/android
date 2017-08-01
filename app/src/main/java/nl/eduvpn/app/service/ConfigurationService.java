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

package nl.eduvpn.app.service;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import nl.eduvpn.app.BuildConfig;
import nl.eduvpn.app.entity.ConnectionType;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.InstanceList;
import nl.eduvpn.app.entity.exception.InvalidSignatureException;
import nl.eduvpn.app.utils.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Service which provides the app configuration.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class ConfigurationService extends java.util.Observable {

    private static final String TAG = ConfigurationService.class.getName();

    private final SerializerService _serializerService;
    private final PreferencesService _preferencesService;
    private final SecurityService _securityService;
    private final OkHttpClient _okHttpClient;

    private InstanceList _secureInternetList;
    private InstanceList _instituteAccessList;

    public ConfigurationService(PreferencesService preferencesService, SerializerService serializerService,
                                SecurityService securityService, OkHttpClient okHttpClient) {
        _preferencesService = preferencesService;
        _serializerService = serializerService;
        _securityService = securityService;
        _okHttpClient = okHttpClient;
        _loadSavedLists();
        _fetchLatestConfiguration();
    }

    /**
     * Returns the instance list configuration.
     *
     * @return The instance list configuration.
     */
    @NonNull
    public List<Instance> getSecureInternetList() {
        if (_secureInternetList == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(_secureInternetList.getInstanceList());
        }
    }

    @NonNull
    public List<Instance> getInstituteAccessList() {
        if (_instituteAccessList == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(_instituteAccessList.getInstanceList());
        }
    }

    private void _loadSavedLists() {
        // Loads the saved configuration from the storage.
        // If none found, it will default to the one in the app.
        _secureInternetList = _preferencesService.getInstanceList(ConnectionType.SECURE_INTERNET);
        _instituteAccessList = _preferencesService.getInstanceList(ConnectionType.INSTITUTE_ACCESS);
    }

    private void _saveListIfChanged(@NonNull InstanceList instanceList, @ConnectionType int connectionType) {
        InstanceList previousList = _preferencesService.getInstanceList(connectionType);
        if (previousList == null || previousList.getSequenceNumber() < instanceList.getSequenceNumber()) {
            Log.i(TAG, "Previously saved instance list for connection type " + connectionType + " is outdated, or there was" +
                    " no existing one. Saving new list for the future.");
            _preferencesService.storeInstanceList(connectionType, instanceList);
            setChanged();
            notifyObservers();
            clearChanged();
        } else {
            Log.d(TAG, "Previously saved instance list for connection type " + connectionType + " has the same version as " +
                    "the newly downloaded one, new one does not have to be cached.");
        }
    }

    /**
     * Parses the JSON string of the instance list to a POJO object.
     *
     * @param instanceListString The string with the JSON representation.
     * @param connectionType     The connection types for these instances.
     * @return An InstanceList object containing the same information.
     * @throws JSONException Thrown if the JSON was malformed or had an unknown list version.
     */
    private InstanceList _parseInstanceList(String instanceListString, @ConnectionType int connectionType) throws Exception {
        JSONObject instanceListJson = new JSONObject(instanceListString);
        InstanceList result = _serializerService.deserializeInstanceList(instanceListJson);
        for (Instance instance : result.getInstanceList()) {
            instance.setConnectionType(connectionType);
        }
        return result;
    }

    /**
     * Downloads, parses, and saves the latest configuration retrieved from the URL defined in the build configuration.
     */
    private void _fetchLatestConfiguration() {
        _fetchConfigurationForConnectionType(ConnectionType.INSTITUTE_ACCESS);
        _fetchConfigurationForConnectionType(ConnectionType.SECURE_INTERNET);
    }

    private void _fetchConfigurationForConnectionType(@ConnectionType final int connectionType) {
        Observable<String> instanceListObservable = _createInstanceListObservable(connectionType);
        Observable<String> signatureObservable = _createSignatureObservable(connectionType);
        // Combine the result of the two
        Observable.zip(instanceListObservable, signatureObservable, new BiFunction<String, String, InstanceList>() {
            @Override
            public InstanceList apply(@io.reactivex.annotations.NonNull String instanceList, @io.reactivex.annotations.NonNull String signature) throws Exception {
                if (_securityService.isValidSignature(instanceList, signature)) {
                    return _parseInstanceList(instanceList, connectionType);
                } else {
                    throw new InvalidSignatureException("Signature validation failed for instance list! Connection type: " + connectionType);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<InstanceList>() {
                    @Override
                    public void accept(InstanceList instanceList) throws Exception {
                        if (connectionType == ConnectionType.SECURE_INTERNET) {
                            _secureInternetList = instanceList;
                        } else {
                            _instituteAccessList = instanceList;
                        }
                        _saveListIfChanged(instanceList, connectionType);
                        Log.i(TAG, "Successfully refreshed instance list for connection type: " + connectionType);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, "Error encountered while fetching instance list for connection type: " + connectionType, throwable);
                    }
                });
    }

    private Observable<String> _createSignatureObservable(@ConnectionType final int connectionType) {
        return Observable.defer(new Callable<ObservableSource<String>>() {
            @Override
            public ObservableSource<String> call() throws Exception {
                String signatureRequestUrl = connectionType == ConnectionType.SECURE_INTERNET ? BuildConfig.INSTANCE_LIST_URL : BuildConfig.FEDERATION_LIST_URL;
                signatureRequestUrl = signatureRequestUrl + BuildConfig.SIGNATURE_URL_POSTFIX;
                Request request = new Request.Builder().url(signatureRequestUrl).build();
                Response response = _okHttpClient.newCall(request).execute();
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    //noinspection WrongConstant
                    return Observable.just(responseBody.string());
                } else {
                    return Observable.error(new IOException("Response body is empty!"));
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<String> _createInstanceListObservable(@ConnectionType final int connectionType) {
        return Observable.defer(new Callable<ObservableSource<String>>() {
            @Override
            public ObservableSource<String> call() throws Exception {
                String listRequestUrl = connectionType == ConnectionType.SECURE_INTERNET ? BuildConfig.INSTANCE_LIST_URL : BuildConfig.FEDERATION_LIST_URL;
                Request request = new Request.Builder().url(listRequestUrl).build();
                Response response = _okHttpClient.newCall(request).execute();
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    //noinspection WrongConstant
                    return Observable.just(responseBody.string());
                } else {
                    return Observable.error(new IOException("Response body is empty!"));
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }
}
