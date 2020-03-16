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
import io.reactivex.schedulers.Schedulers;
import nl.eduvpn.app.BuildConfig;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.InstanceList;
import nl.eduvpn.app.entity.Organization;
import nl.eduvpn.app.entity.exception.InvalidSignatureException;
import nl.eduvpn.app.utils.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Service which provides the configurations for organization related data model.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class OrganizationService extends java.util.Observable {

    private static final String TAG = OrganizationService.class.getName();

    private final SerializerService _serializerService;
    private final PreferencesService _preferencesService;
    private final SecurityService _securityService;
    private final OkHttpClient _okHttpClient;

    // List of organizations the user can connect to
    private List<Organization> _organizationList;

    private boolean _organizationsPendingDiscovery = true;

    public OrganizationService(PreferencesService preferencesService, SerializerService serializerService,
                               SecurityService securityService, OkHttpClient okHttpClient) {
        _preferencesService = preferencesService;
        _serializerService = serializerService;
        _securityService = securityService;
        _okHttpClient = okHttpClient;
        _loadSavedLists();
        if (BuildConfig.API_DISCOVERY_ENABLED) {
            _fetchLatestConfiguration();
        }
    }

    /**
     * Returns the list of organizations.
     *
     * @return The currently cached list of organizations.
     */
    @NonNull
    public List<Organization> getOrganizationList() {
        if (_organizationList == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(_organizationList);
        }
    }

    private void _loadSavedLists() {
        // Loads the saved configuration from the storage.
        // If none found, it will default to the one in the app.
        _organizationList = _preferencesService.getOrganizationList();
    }

    private void _saveList(@NonNull List<Organization> organizationList) {
        _preferencesService.storeOrganizationList(organizationList);
        setChanged();
        notifyObservers();
        clearChanged();
    }


    /**
     * Parses the JSON string of the instance list to a POJO object.
     *
     * @param organizationList The string with the JSON representation.
     * @return An InstanceList object containing the same information.
     * @throws JSONException Thrown if the JSON was malformed or had an unknown list version.
     */
    private List<Organization> _parseOrganizationList(String organizationList) throws Exception {
        JSONObject organizationListJson = new JSONObject(organizationList);
        return _serializerService.deserializeOrganizationList(organizationListJson);
    }

    /**
     * Downloads, parses, and saves the latest configuration retrieved from the URL defined in the build configuration.
     */
    private void _fetchLatestConfiguration() {
            _organizationsPendingDiscovery = true;
            _fetchOrganizations();
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
                    _saveList(instanceList);
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
}
