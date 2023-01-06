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
import androidx.lifecycle.LiveData;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationServiceConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import kotlin.Pair;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.Organization;
import nl.eduvpn.app.entity.SavedAuthState;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.utils.Log;

/**
 * Service which stores previously used access token and profile names.
 * This allows us to skip some steps, which will make the user experience more fluid.
 * Created by Daniel Zolnai on 2016-10-20.
 */
public class HistoryService extends LiveData<Void> {
    private static final String TAG = HistoryService.class.getName();

    private List<SavedAuthState> _savedAuthStateList;
    private List<SavedKeyPair> _savedKeyPairList;

    private Organization _savedOrganization;

    private final PreferencesService _preferencesService;

    /**
     * Constructor.
     *
     * @param preferencesService The preferences service which stores the app state.
     */
    public HistoryService(@NonNull PreferencesService preferencesService) {
        _preferencesService = preferencesService;
        _load();
    }

    /**
     * Loads the state of the service.
     */
    private void _load() {
        _savedAuthStateList = _preferencesService.getSavedAuthStateList();
        if (_savedAuthStateList == null) {
            _savedAuthStateList = new ArrayList<>();
            Log.i(TAG, "No saved tokens found.");
        }
        _savedKeyPairList = _preferencesService.getSavedKeyPairList();
        if (_savedKeyPairList == null) {
            Log.i(TAG, "No saved key pair found.");
            _savedKeyPairList = new ArrayList<>();
        }
        _savedOrganization = _preferencesService.getSavedOrganization();
    }

    /**
     * Saves the state of the service.
     */
    private void _save() {
        _preferencesService.storeSavedAuthStateList(_savedAuthStateList);
        _preferencesService.storeSavedOrganization(_savedOrganization);
    }

    /**
     * Returns a cached authorization state for an API.
     *
     * @param instance The instance to get the access token for.
     * @return The authorization state if found. Null if not found.
     */
    @Nullable
    public Pair<AuthState, Date> getCachedAuthState(@NonNull Instance instance) {
        for (SavedAuthState savedAuthState : _savedAuthStateList) {
            if (savedAuthState.getInstance()
                    .getSanitizedBaseURI()
                    .equals(instance.getSanitizedBaseURI())) {
                return new Pair(savedAuthState.getAuthState(), savedAuthState.getAuthenticationDate());
            } else if (instance.getAuthorizationType() == AuthorizationType.Distributed && savedAuthState
                    .getInstance()
                    .getAuthorizationType() == AuthorizationType.Distributed) {
                return new Pair(savedAuthState.getAuthState(), savedAuthState.getAuthenticationDate());
            }
        }
        return null;
    }

    /**
     * Caches an access token for an API.
     *
     * @param instance  The VPN provider the token is stored for.
     * @param authState The authorization state which contains the access and refresh tokens.
     */
    public void cacheAuthorizationState(@NonNull Instance instance, @NonNull AuthState authState, @Nullable Date authenticationDate) {
        List<Instance> existingInstances = new ArrayList<>();
        if (instance.getAuthorizationType() == AuthorizationType.Distributed) {
            for (SavedAuthState savedAuthState : _savedAuthStateList) {
                if (savedAuthState.getInstance()
                        .getAuthorizationType() == AuthorizationType.Distributed && !savedAuthState.getInstance()
                        .getSanitizedBaseURI()
                        .equals(instance.getSanitizedBaseURI())) {
                    existingInstances.add(savedAuthState.getInstance());
                }
            }
        }
        // Remove all previous entries
        _removeAuthorizations(instance);
        _savedAuthStateList.add(new SavedAuthState(instance, authState, authenticationDate));
        for (Instance existingSharedInstance : existingInstances) {
            _savedAuthStateList.add(new SavedAuthState(existingSharedInstance, authState, authenticationDate));
        }
        _save();
        postValue(null);
    }

    /**
     * Removes the access token(s) which have the given base URI.
     *
     * @param instance The instance the access token will be saved for.
     */
    private void _removeAuthorizations(@NonNull Instance instance) {
        Iterator<SavedAuthState> savedTokenIterator = _savedAuthStateList.iterator();
        while (savedTokenIterator.hasNext()) {
            SavedAuthState savedAuthState = savedTokenIterator.next();
            if (instance.getAuthorizationType() == AuthorizationType.Distributed &&
                    savedAuthState.getInstance().getAuthorizationType() == AuthorizationType.Distributed) {
                savedTokenIterator.remove();
                Log.i(TAG, "Deleted saved token for distributed auth instance " + savedAuthState.getInstance().getSanitizedBaseURI());
            } else if (instance.getAuthorizationType() == AuthorizationType.Local &&
                    savedAuthState.getInstance().getSanitizedBaseURI().equals(instance.getSanitizedBaseURI())) {
                savedTokenIterator.remove();
                Log.i(TAG, "Deleted saved token for local auth instance " + savedAuthState.getInstance().getSanitizedBaseURI());
            } else if (instance.getAuthorizationType() == AuthorizationType.Organization &&
                    savedAuthState.getInstance().getSanitizedBaseURI().equals(instance.getSanitizedBaseURI())) {
                savedTokenIterator.remove();
                Log.i(TAG, "Deleted saved token for organization auth instance " + savedAuthState.getInstance().getSanitizedBaseURI());
            }
        }
        _save();
    }

    /**
     * Returns the list of all saved tokens.
     *
     * @return The list of all saved access tokens and instances.
     */
    public List<SavedAuthState> getSavedAuthStateList() {
        return Collections.unmodifiableList(_savedAuthStateList);
    }

    /**
     * Returns a saved token for a given sanitized base URI.
     *
     * @param instance The instance to get the token for.
     * @return The token if available, otherwise null.
     */
    @Nullable
    public SavedAuthState getSavedToken(Instance instance) {
        // First we prioritize tokens which belong to the same instance
        for (SavedAuthState savedAuthState : _savedAuthStateList) {
            if (instance.getSanitizedBaseURI().equals(savedAuthState.getInstance().getSanitizedBaseURI())) {
                return savedAuthState;
            }
        }
        // Second pass: if distributed auth instance, any other instance with distributed auth is fine as well
        if (instance.getAuthorizationType() == AuthorizationType.Distributed) {
            for (SavedAuthState savedAuthState : _savedAuthStateList) {
                if (savedAuthState.getInstance().getAuthorizationType() == AuthorizationType.Distributed) {
                    return savedAuthState;
                }
            }
        }
        return null;
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
        return _savedOrganization;
    }

    public void storeSavedOrganization(@NonNull Organization organization) {
        _savedOrganization = organization;
        _preferencesService.storeSavedOrganization(organization);
        postValue(null);
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
     * Refreshes an auth state in the list.
     *
     * @param authState The auth state to refresh.
     */
    public void refreshAuthState(AuthState authState) {
        if (_savedAuthStateList == null) {
            Log.w(TAG, "No saved auth states found. Nothing to refresh?");
            return;
        }
        // Two auth states are for the same API if their configuration is the same.
        AuthorizationServiceConfiguration currentConfig = authState.getAuthorizationServiceConfiguration();
        for (SavedAuthState savedAuthState : _savedAuthStateList) {
            if (_authConfigsEqual(currentConfig, savedAuthState.getAuthState().getAuthorizationServiceConfiguration())) {
                savedAuthState.setAuthState(authState);
                Log.d(TAG, "Auth state found and replaced for " + savedAuthState.getInstance().getBaseURI());
                break;
            }
        }
        _preferencesService.storeSavedAuthStateList(_savedAuthStateList);
    }

    /**
     * Checks if two authorization service configs are equal
     *
     * @param left  The first operand.
     * @param right The second operand.
     * @return True if they have the same URLs, false if not.
     */
    private boolean _authConfigsEqual(AuthorizationServiceConfiguration left, AuthorizationServiceConfiguration right) {
        return left.tokenEndpoint.toString().equals(right.tokenEndpoint.toString()) &&
                left.authorizationEndpoint.toString().equals(right.authorizationEndpoint.toString());

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
        _save();
    }

    /**
     * Removes all saved data for an instance.
     *
     * @param instance The instance to remove the data for.
     */
    public void removeAllDataForInstance(Instance instance) {
        if (instance.getAuthorizationType() == AuthorizationType.Distributed) {
            // Remove all distributed instance related data
            List<SavedAuthState> authStates = new ArrayList<>(getSavedAuthStateList());
            for (SavedAuthState savedAuthState : authStates) {
                if (savedAuthState.getInstance().getAuthorizationType() == AuthorizationType.Distributed) {
                    removeSavedKeyPairs(savedAuthState.getInstance());
                    _removeAuthorizations(savedAuthState.getInstance());
                }
            }
        } else {
            removeSavedKeyPairs(instance);
            _removeAuthorizations(instance);
        }
        postValue(null);
    }

    /***
     * Removes all saved data in this app.
     ***/
    public void removeOrganizationData() {
        _savedOrganization = null;
        _preferencesService.setCurrentOrganization(null);
        List<Instance> instancesToRemove = new ArrayList<>();
        for (SavedAuthState authState : _savedAuthStateList) {
            if (authState.getInstance() != null && authState.getInstance().getAuthorizationType() == AuthorizationType.Distributed) {
                instancesToRemove.add(authState.getInstance());
            }
        }
        for (Instance instance : instancesToRemove) {
            removeAllDataForInstance(instance);
        }
        _save();
    }
}
