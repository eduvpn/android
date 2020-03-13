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

import android.annotation.SuppressLint;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import nl.eduvpn.app.BuildConfig;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.InstanceList;
import nl.eduvpn.app.entity.OrganizationList;
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

    private OrganizationList _organizationList;
    private InstanceList _secureInternetList;
    private InstanceList _instituteAccessList;


    private boolean _organizationsPendingDiscovery = true;
    private boolean _secureInternetPendingDiscovery = true;
    private boolean _instituteAccessPendingDiscovery = true;


    public ConfigurationService(PreferencesService preferencesService, SerializerService serializerService,
                                SecurityService securityService, OkHttpClient okHttpClient) {
        _preferencesService = preferencesService;
        _serializerService = serializerService;
        _securityService = securityService;
        _okHttpClient = okHttpClient;
        _loadSavedLists();
        if (BuildConfig.API_DISCOVERY_ENABLED) {
            _fetchLatestConfiguration();
        } else {
            // Otherwise the user can only enter custom URLs.
            if (BuildConfig.NEW_ORGANIZATION_LIST_ENABLED) {
                _organizationsPendingDiscovery = false;
            } else {
                _secureInternetPendingDiscovery = false;
                _instituteAccessPendingDiscovery = false;
            }
        }

    }

    /**
     * Returns the list of organizations.
     *
     * @return The currently cached list of organizations.
     */
    @NonNull
    public List<Instance> getOrganizationList() {
        if (_organizationList == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(_organizationList.getInstanceList());
        }
    }

    private void _loadSavedLists() {
        // Loads the saved configuration from the storage.
        // If none found, it will default to the one in the app.
        if (BuildConfig.NEW_ORGANIZATION_LIST_ENABLED) {
            _organizationList = _preferencesService.getOrganizationList();
        } else {
            _secureInternetList = _preferencesService.getInstanceList(AuthorizationType.Distributed);
            _instituteAccessList = _preferencesService.getInstanceList(AuthorizationType.Local);
        }
    }

    private void _saveListIfChanged(@NonNull OrganizationList organizationList) {
        OrganizationList previousList = _preferencesService.getOrganizationList();
        if (previousList == null || previousList.getSequenceNumber() < organizationList.getSequenceNumber()) {
            Log.i(TAG, "Previously saved organization list is outdated, or there was" +
                    " no existing one. Saving new list for the future.");
            _preferencesService.storeOrganizationList(organizationList);
            setChanged();
            notifyObservers();
            clearChanged();
        } else {
            Log.d(TAG, "Previously saved organization list has the same version as " +
                    "the newly downloaded one, new one does not have to be cached.");
        }
    }

    private void _saveListIfChanged(@NonNull InstanceList instanceList, AuthorizationType authorizationType) {
        InstanceList previousList = _preferencesService.getInstanceList(authorizationType);
        if (previousList == null || previousList.getSequenceNumber() < instanceList.getSequenceNumber()) {
            Log.i(TAG, "Previously saved instance list for connection type " + authorizationType + " is outdated, or there was" +
                    " no existing one. Saving new list for the future.");
            _preferencesService.storeInstanceList(authorizationType, instanceList);
            setChanged();
            notifyObservers();
            clearChanged();
        } else {
            Log.d(TAG, "Previously saved instance list for connection type " + authorizationType + " has the same version as " +
                    "the newly downloaded one, new one does not have to be cached.");
        }
    }

    /**
     * Parses the JSON string of the instance list to a POJO object.
     *
     * @param organizationList The string with the JSON representation.
     * @return An InstanceList object containing the same information.
     * @throws JSONException Thrown if the JSON was malformed or had an unknown list version.
     */
    private OrganizationList _parseOrganizationList(String organizationList) throws Exception {
        JSONObject organizationListJson = new JSONObject(organizationList);
        // TODO do it with organization list
        //return _serializerService.deserializeOrganizationList(organizationListJson);
        return null;
    }

    /**
     * Parses the JSON string of the instance list to a POJO object.
     *
     * @param instanceListString The string with the JSON representation.
     * @param authorizationType     The authorization types for these instances.
     * @return An InstanceList object containing the same information.
     * @throws JSONException Thrown if the JSON was malformed or had an unknown list version.
     */
    @Deprecated
    private InstanceList _parseInstanceList(String instanceListString, AuthorizationType authorizationType) throws Exception {
        JSONObject instanceListJson = new JSONObject(instanceListString);
        InstanceList result = _serializerService.deserializeInstanceList(instanceListJson);
        for (Instance instance : result.getInstanceList()) {
            instance.setAuthorizationType(authorizationType);
        }
        return result;
    }

    /**
     * Downloads, parses, and saves the latest configuration retrieved from the URL defined in the build configuration.
     */
    private void _fetchLatestConfiguration() {
        if (BuildConfig.NEW_ORGANIZATION_LIST_ENABLED) {
            _organizationsPendingDiscovery = true;
            _fetchOrganizations();
        } else {
            _secureInternetPendingDiscovery = true;
            _instituteAccessPendingDiscovery = true;
            _fetchConfigurationForAuthorizationType(AuthorizationType.Distributed);
            _fetchConfigurationForAuthorizationType(AuthorizationType.Local);
        }
    }

    @SuppressLint("CheckResult")
    private void _fetchConfigurationForAuthorizationType(AuthorizationType authorizationType) {
        // Combine the result of the two
        Observable<String> instanceListObservable = _createInstanceListObservable(authorizationType);
        String listRequestUrl = authorizationType == AuthorizationType.Local ? BuildConfig.INSTITUTE_ACCESS_DISCOVERY_URL : BuildConfig.SECURE_INTERNET_DISCOVERY_URL;
        Observable<String> signatureObservable = _createSignatureObservable(listRequestUrl);
        // Combine the result of the two
        Observable.zip(instanceListObservable, signatureObservable, (BiFunction<String, String, InstanceList>)(instanceList, signature) -> {
            if (_securityService.verifyDeprecatedSignature(instanceList, signature)) {
                return _parseInstanceList(instanceList, authorizationType);
            } else {
                throw new InvalidSignatureException("Signature validation failed for instance list! Authorization type: " + authorizationType);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(instanceList -> {
                    if (authorizationType == AuthorizationType.Distributed) {
                        _secureInternetList = instanceList;
                        _secureInternetPendingDiscovery = false;
                    } else {
                        _instituteAccessList = instanceList;
                        _instituteAccessPendingDiscovery = false;
                    }
                    _saveListIfChanged(instanceList, authorizationType);
                    Log.i(TAG, "Successfully refreshed instance list for authorization type: " + authorizationType);
                }, throwable -> {
                    if (authorizationType == AuthorizationType.Distributed) {
                        _secureInternetPendingDiscovery = false;
                    } else {
                        _instituteAccessPendingDiscovery = false;
                    }
                    setChanged();
                    notifyObservers();
                    clearChanged();
                    Log.e(TAG, "Error encountered while fetching instance list for authorization type: " + authorizationType, throwable);
                });
    }

    @SuppressLint("CheckResult")
    private void _fetchOrganizations() {
        Observable<String> organizationListObservable = _createOrganizationListObservable();
        Observable<String> signatureObservable = _createSignatureObservable(BuildConfig.ORGANIZATION_LIST_URL);
        Observable.zip(organizationListObservable, signatureObservable, (instanceList, signature) -> {
            try {
                if (_securityService.verifyMinisign(instanceList, signature)) {
                    return _parseOrganizationList(instanceList);
                } else {
                    throw new InvalidSignatureException("Signature validation failed for organization list!");
                }
            } catch (Exception ex) {
                return _parseOrganizationList(instanceList);
                // TODO: replace line above with line below [see README.md why]
                // throw new InvalidSignatureException("Signature validation failed for organization list!");
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(instanceList -> {
                    _organizationList = instanceList;
                    _organizationsPendingDiscovery = false;
                    _saveListIfChanged(instanceList);
                    Log.i(TAG, "Successfully refreshed organization list.");
                }, throwable -> {
                    _organizationsPendingDiscovery = false;
                    setChanged();
                    notifyObservers();
                    clearChanged();
                    Log.e(TAG, "Error encountered while fetching organization list", throwable);
                });
    }

    private Observable<String> _createSignatureObservable(final String signatureRequestUrl) {
        return Observable.defer((Callable<ObservableSource<String>>)() -> {
            String postfixedUrl = signatureRequestUrl + BuildConfig.SIGNATURE_URL_POSTFIX;
            Request request = new Request.Builder().url(postfixedUrl).build();
            Response response = _okHttpClient.newCall(request).execute();
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                //noinspection WrongConstant
                String result = responseBody.string();
                responseBody.close();
                return Observable.just(result);
            } else {
                return Observable.error(new IOException("Response body is empty!"));
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<String> _createInstanceListObservable(AuthorizationType authorizationType) {
        return Observable.defer((Callable<ObservableSource<String>>)() -> {
            String listRequestUrl = authorizationType == AuthorizationType.Local ? BuildConfig.INSTITUTE_ACCESS_DISCOVERY_URL : BuildConfig.SECURE_INTERNET_DISCOVERY_URL;
            Request request = new Request.Builder().url(listRequestUrl).build();
            Response response = _okHttpClient.newCall(request).execute();
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                //noinspection WrongConstant
                String result = responseBody.string();
                responseBody.close();
                return Observable.just(result);
            } else {
                return Observable.error(new IOException("Response body is empty!"));
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<String> _createOrganizationListObservable() {
        return Observable.defer((Callable<ObservableSource<String>>)() -> {
            String listRequestUrl = BuildConfig.ORGANIZATION_LIST_URL;
            Request request = new Request.Builder().url(listRequestUrl).build();
            Response response = _okHttpClient.newCall(request).execute();
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                //noinspection WrongConstant
                String result = responseBody.string();
                responseBody.close();
                return Observable.just(result);
            } else {
                return Observable.error(new IOException("Response body is empty!"));
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns if organization discovery is still pending.
     *
     * @return True if discovery is still not completed, and that's why the list is not complete.
     */
    public boolean isPendingOrganizationsDiscovery() {
        return _organizationsPendingDiscovery;
    }

    /**
     * Returns if provider discovery is still pending for a given authorization type.
     * @param authorizationType The authorization type.
     * @return True if discovery is still not completed, and that's why the list is not complete.
     */
    public boolean isPendingDiscovery(AuthorizationType authorizationType) {
        if (authorizationType == AuthorizationType.Local) {
            return _instituteAccessPendingDiscovery;
        } else {
            return _secureInternetPendingDiscovery;
        }
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

    /**
     * Returns the instance list configuration.
     *
     * @return The instance list configuration.
     */
    @NonNull
    public List<Instance> getInstituteAccessList() {
        if (_instituteAccessList == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(_instituteAccessList.getInstanceList());
        }
    }

}
