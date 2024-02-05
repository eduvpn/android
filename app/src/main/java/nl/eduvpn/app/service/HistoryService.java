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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import nl.eduvpn.app.entity.AddedServers;
import nl.eduvpn.app.entity.CertExpiryTimes;
import nl.eduvpn.app.entity.CurrentServer;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.Organization;
import nl.eduvpn.app.entity.OrganizationList;
import nl.eduvpn.app.entity.exception.CommonException;
import nl.eduvpn.app.utils.Listener;
import nl.eduvpn.app.utils.Log;

/**
 * Service which stores previously used access token and profile names.
 * This allows us to skip some steps, which will make the user experience more fluid.
 * Created by Daniel Zolnai on 2016-10-20.
 */
public class HistoryService {
    private static final String TAG = HistoryService.class.getName();
    private AddedServers _addedServers = null;

    private final BackendService _backendService;

    private List<Listener> _listeners = new LinkedList<>();

    private @Nullable OrganizationList _memoryCachedOrganizationList = null;

    /**
     * Constructor.
     */
    public HistoryService(@NonNull BackendService backendService) {
        _backendService = backendService;
    }

    /**
     * Loads the state of the service.
     */
    public void load() {
        try {
            _addedServers = _backendService.getAddedServers();
            notifyListeners();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addListener(Listener listener) {
        if (!_listeners.contains(listener)) {
            _listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        _listeners.remove(listener);
    }

    private void notifyListeners() {
        _listeners.forEach(l -> l.update(this, null));
    }


    public @Nullable AddedServers getAddedServers() {
        return _addedServers;
    }

    public CurrentServer getCurrentServer() {
        return _backendService.getCurrentServer();
    }

    public @Nullable CertExpiryTimes getCertExpiryTimes() {
        try {
            return _backendService.getCertExpiryTimes();
        } catch (Exception ex) {
            Log.w(TAG, "Could not determine cert expiry times!", ex);
            return null;
        }
    }

    public @Nullable OrganizationList getOrganizationList() {
        return _memoryCachedOrganizationList;
    }

    public boolean hasSecureInternetServer() {
        return _addedServers.getSecureInternetServer() != null;
    }

    public void setOrganizationList(@Nullable OrganizationList organizationList) {
        _memoryCachedOrganizationList = organizationList;
    }

    /**
     * Removes all saved data for an instance.
     *
     * @param instance The instance to remove the data for.
     */
    public void removeAllDataForInstance(Instance instance) throws CommonException {
        _backendService.removeServer(instance);
        load();
        notifyListeners();
    }

    /***
     * Removes all saved data in this app.
     ***/
    public void removeOrganizationData() throws CommonException, SerializerService.UnknownFormatException {
        List<Instance> instancesToRemove = _backendService.getAddedServers().asInstances();
        CommonException errorThrown = null;
        for (Instance instance : instancesToRemove) {
            try {
                removeAllDataForInstance(instance);
            } catch (CommonException ex) {
                errorThrown = ex;
            }
        }
        if (errorThrown != null) {
            throw errorThrown;
        }
    }
}
