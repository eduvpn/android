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

import org.eduvpn.common.CommonException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import nl.eduvpn.app.entity.AddedServers;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.Organization;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.utils.Listener;
import nl.eduvpn.app.utils.Log;

/**
 * Service which stores previously used access token and profile names.
 * This allows us to skip some steps, which will make the user experience more fluid.
 * Created by Daniel Zolnai on 2016-10-20.
 */
public class HistoryService {
    private static final String TAG = HistoryService.class.getName();

    private List<SavedKeyPair> _savedKeyPairList = new ArrayList<>();

    private AddedServers _addedServers = null;

    private final PreferencesService _preferencesService;

    private final BackendService _backendService;

    private List<Listener> _listeners = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param preferencesService The preferences service which stores the app state.
     */
    public HistoryService(@NonNull PreferencesService preferencesService,
                          @NonNull BackendService backendService) {
        _preferencesService = preferencesService;
        _backendService = backendService;
    }

    /**
     * Loads the state of the service.
     */
    public void load() {
        try {
            _addedServers = _backendService.getAddedServers();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        _savedKeyPairList = _preferencesService.getSavedKeyPairList();
        if (_savedKeyPairList == null) {
            Log.i(TAG, "No saved key pair found.");
            _savedKeyPairList = new ArrayList<>();
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

    /**
     * Returns a saved key pair for an instance.
     *
     * @param instance The instance the key pair was created for.
     * @return The saved key pair if there was a previously generated one. Null if none created yet.
     */
    public SavedKeyPair getSavedKeyPairForInstance(Instance instance) {
        for (SavedKeyPair savedKeyPair : _savedKeyPairList) {
            if (savedKeyPair.getInstance().getSanitizedBaseURI().equals(instance.getSanitizedBaseURI())) {
                return savedKeyPair;
            }
        }
        return null;
    }

    @Nullable
    public Organization getSavedOrganization() {
        if (_addedServers == null) {
            return null;
        }
        return _addedServers.getSecureInternetServer();
    }

    /**
     * Stores a saved key pair so it can be retrieved next time.
     *
     * @param savedKeyPair The saved key pair to store.
     */
    public void storeSavedKeyPair(@NonNull SavedKeyPair savedKeyPair) {
        // Check if it is not a duplicate
        boolean wasDuplicate = false;
        ListIterator<SavedKeyPair> savedKeyPairIterator = _savedKeyPairList.listIterator();
        while (savedKeyPairIterator.hasNext()) {
            SavedKeyPair current = savedKeyPairIterator.next();
            if (current.getInstance().getSanitizedBaseURI().equals(savedKeyPair.getInstance().getSanitizedBaseURI())) {
                if (!wasDuplicate) {
                    savedKeyPairIterator.set(savedKeyPair);
                } else {
                    // We already replaced one. So this one is a duplicate.
                    Log.w(TAG, "Found a duplicate key pair entry! Removing second one.");
                    savedKeyPairIterator.remove();
                }
                wasDuplicate = true;
            }
        }
        if (!wasDuplicate) {
            _savedKeyPairList.add(savedKeyPair);
        }
        _preferencesService.storeSavedKeyPairList(_savedKeyPairList);
    }

    /**
     * Removes the saved key pairs for the instance and all connecting instances.
     *
     * @param instance The instance to remove.
     */
    public void removeSavedKeyPairs(Instance instance) {
        if (_savedKeyPairList == null) {
            Log.i(TAG, "No saved key pairs found to remove.");
            return;
        }
        ListIterator<SavedKeyPair> keyPairListIterator = _savedKeyPairList.listIterator();
        while (keyPairListIterator.hasNext()) {
            SavedKeyPair current = keyPairListIterator.next();
            if (instance.getAuthorizationType() == AuthorizationType.Distributed &&
                    current.getInstance().getAuthorizationType() == AuthorizationType.Distributed) {
                keyPairListIterator.remove();
                Log.i(TAG, "Deleted saved key pair for distributed auth instance " + current.getInstance().getSanitizedBaseURI());
            } else if (instance.getAuthorizationType() == AuthorizationType.Local &&
                    instance.getSanitizedBaseURI().equals(current.getInstance().getSanitizedBaseURI())) {
                keyPairListIterator.remove();
                Log.i(TAG, "Deleted saved key pair for local auth instance " + current.getInstance().getSanitizedBaseURI());

            }
        }
    }

    /**
     * Removes all saved data for an instance.
     *
     * @param instance The instance to remove the data for.
     */
    public void removeAllDataForInstance(Instance instance) throws CommonException {
        _backendService.removeServer(instance);
        notifyListeners();
    }

    /***
     * Removes all saved data in this app.
     ***/
    public void removeOrganizationData() {
        _preferencesService.setCurrentOrganization(null);
        List<Instance> instancesToRemove = new ArrayList<>();
        CommonException errorThrown = null;
        for (Instance instance : instancesToRemove) {
            try {
                removeAllDataForInstance(instance);
            } catch (CommonException ex) {
                errorThrown = ex;
            }
        }
        if (errorThrown != null) {
            // TODO handle
        }
    }

    public Date getAuthenticationDateForCurrentInstance() {
        // TODO
        return new Date();
    }
}
