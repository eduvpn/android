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
import nl.eduvpn.app.Constants;
import nl.eduvpn.app.entity.AuthorizationType;
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
            //_fetchLatestConfiguration();
        } else {
            // Otherwise the user can only enter custom URLs.
            _secureInternetPendingDiscovery = false;
            _instituteAccessPendingDiscovery = false;
        }

    }

    private void _loadSavedLists() {
        // Loads the saved configuration from the storage.
        // If none found, it will default to the one in the app.

        _secureInternetList = _preferencesService.getInstanceList(AuthorizationType.Distributed);
        _instituteAccessList = _preferencesService.getInstanceList(AuthorizationType.Local);
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
     * Returns if provider discovery is still pending for a given authorization type.
     *
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
