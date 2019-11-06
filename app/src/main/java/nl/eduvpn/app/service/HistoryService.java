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

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationServiceConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Observable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.SavedAuthState;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.entity.SavedProfile;
import nl.eduvpn.app.utils.Log;
import nl.eduvpn.app.utils.TTLCache;

/**
 * Service which stores previously used access token and profile names.
 * This allows us to skip some steps, which will make the user experience more fluid.
 * Created by Daniel Zolnai on 2016-10-20.
 */
public class HistoryService extends Observable {
    private static final String TAG = HistoryService.class.getName();

    private List<SavedProfile> _savedProfileList;
    private List<SavedAuthState> _savedAuthStateList;
    private List<SavedKeyPair> _savedKeyPairList;

    private final PreferencesService _preferencesService;

    public static final Integer NOTIFICATION_TOKENS_CHANGED = 1;
    public static final Integer NOTIFICATION_PROFILES_CHANGED = 2;

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
        _savedProfileList = _preferencesService.getSavedProfileList();
        if (_savedProfileList == null) {
            _savedProfileList = new ArrayList<>();
            Log.i(TAG, "No saved profiles found.");
        }
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
    }

    /**
     * Saves the state of the service.
     */
    private void _save() {
        _preferencesService.storeSavedProfileList(_savedProfileList);
        _preferencesService.storeSavedAuthStateList(_savedAuthStateList);
    }

    /**
     * Returns a cached authentication state for an API.
     *
     * @param instance The instance to get the access token for.
     * @return The authentication state if found. Null if not found.
     */
    @Nullable
    public AuthState getCachedAuthState(@NonNull Instance instance) {
        for (SavedAuthState savedAuthState : _savedAuthStateList) {
            if (savedAuthState.getInstance().getSanitizedBaseURI().equals(instance.getSanitizedBaseURI())) {
                return savedAuthState.getAuthState();
            } else if (instance.getAuthorizationType() == AuthorizationType.Distributed && savedAuthState.getInstance().getAuthorizationType() == AuthorizationType.Distributed) {
                return savedAuthState.getAuthState();
            }
        }
        return null;
    }

    /**
     * Caches an access token for an API.
     *
     * @param instance  The VPN provider the token is stored for.
     * @param authState The authentication state which contains the access and refresh tokens.
     */
    public void cacheAuthenticationState(@NonNull Instance instance, @NonNull AuthState authState) {
        // Remove all previous entries
        _removeAuthentications(instance);
        _savedAuthStateList.add(new SavedAuthState(instance, authState));
        _save();
        setChanged();
        notifyObservers(NOTIFICATION_TOKENS_CHANGED);
        clearChanged();
    }

    /**
     * Returns the unmodifiable list of saved profiles.
     *
     * @return The list of saved profiles. If none found, the list will be empty.
     */
    @NonNull
    public List<SavedProfile> getSavedProfileList() {
        return Collections.unmodifiableList(_savedProfileList);
    }

    /**
     * Returns the saved tokens for a specific auth type.
     *
     * @param authorizationType The authorization type filter.
     * @return The list of saved tokens for specific instances.
     */
    @NonNull
    public List<SavedAuthState> getSavedTokensForAuthorizationType(AuthorizationType authorizationType) {
        List<SavedAuthState> result = new ArrayList<>();
        for (SavedAuthState savedAuthState : _savedAuthStateList) {
            if (savedAuthState.getInstance().getAuthorizationType() == authorizationType) {
                result.add(savedAuthState);
            }
        }
        return Collections.unmodifiableList(result);
    }


    /**
     * Stores a saved profile, so the user can select it the next time.
     *
     * @param savedProfile The saved profile to store.
     */
    public void cacheSavedProfile(@NonNull SavedProfile savedProfile) {
        _savedProfileList.add(savedProfile);
        _save();
        setChanged();
        notifyObservers(NOTIFICATION_PROFILES_CHANGED);
        clearChanged();
    }

    /**
     * Returns a cached saved profile by looking it up by the API sanitized base URI and the profile ID.
     *
     * @param sanitizedBaseURI The sanitized base URI of the provider.
     * @param profileId        The unique ID of the profile within the provider.
     * @return The saved profile if found. Null if not found.
     */
    @Nullable
    public SavedProfile getCachedSavedProfile(@NonNull String sanitizedBaseURI, @NonNull String profileId) {
        for (SavedProfile savedProfile : _savedProfileList) {
            if (savedProfile.getInstance().getSanitizedBaseURI().equals(sanitizedBaseURI) &&
                    savedProfile.getProfile().getProfileId().equals(profileId)) {
                return savedProfile;
            }
        }
        return null;
    }

    /**
     * Saves a previously removed profile from the list.
     *
     * @param savedProfile The profile to remove.
     */
    public void removeSavedProfile(@NonNull SavedProfile savedProfile) {
        _savedProfileList.remove(savedProfile);
        _save();
        setChanged();
        notifyObservers(NOTIFICATION_PROFILES_CHANGED);
        clearChanged();
    }

    /**
     * Removes the access token(s) which have the given base URI.
     *
     * @param instance The instance the access token will be saved for.
     */
    private void _removeAuthentications(@NonNull Instance instance) {
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
     * Removes the saved profiles for an instance.
     *
     * @param instance The instance which should be removed.
     */
    public void removeSavedProfilesForInstance(@NonNull Instance instance) {
        Iterator<SavedProfile> savedProfileIterator = _savedProfileList.iterator();
        while (savedProfileIterator.hasNext()) {
            SavedProfile savedProfile = savedProfileIterator.next();
            if (instance.getAuthorizationType() == AuthorizationType.Distributed &&
                    savedProfile.getInstance().getAuthorizationType() == AuthorizationType.Distributed) {
                savedProfileIterator.remove();
                Log.i(TAG, "Deleted profile for distributed auth instance " + savedProfile.getInstance().getSanitizedBaseURI());
            } else if (instance.getAuthorizationType() == AuthorizationType.Local &&
                    savedProfile.getInstance().getSanitizedBaseURI().equals(instance.getSanitizedBaseURI())) {
                savedProfileIterator.remove();
                Log.i(TAG, "Deleted profile for local auth instance " + savedProfile.getInstance().getSanitizedBaseURI());
            }
        }
        _save();
        setChanged();
        notifyObservers(NOTIFICATION_PROFILES_CHANGED);
        clearChanged();
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
        removeSavedKeyPairs(instance);
        _removeAuthentications(instance);
        removeSavedProfilesForInstance(instance); // This will trigger a profiles changed event
    }
}
