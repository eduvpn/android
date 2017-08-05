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
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Observable;

import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.entity.SavedProfile;
import nl.eduvpn.app.entity.SavedToken;
import nl.eduvpn.app.utils.Log;
import nl.eduvpn.app.utils.TTLCache;

/**
 * Service which stores previously used access token and profile names.
 * This allows us to skip some steps, which will make the user experience more fluid.
 * Created by Daniel Zolnai on 2016-10-20.
 */
public class HistoryService extends Observable {
    private static final String TAG = HistoryService.class.getName();

    private static final Long DISCOVERED_API_CACHE_TTL_SECONDS = 30 * 24 * 3600L; // 30 days

    private TTLCache<DiscoveredAPI> _discoveredAPICache;
    private List<SavedProfile> _savedProfileList;
    private List<SavedToken> _savedTokenList;
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
        _discoveredAPICache.purge();
        // Save it immediately, because we just did a purge.
        // (It is better to purge at app start, since we do have some time now).
        _save();
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
        _savedTokenList = _preferencesService.getSavedTokenList();
        if (_savedTokenList == null) {
            _savedTokenList = new ArrayList<>();
            Log.i(TAG, "No saved tokens found.");
        }
        _discoveredAPICache = _preferencesService.getDiscoveredAPICache();
        if (_discoveredAPICache == null) {
            Log.i(TAG, "No discovered API cache found.");
            _discoveredAPICache = new TTLCache<>(DISCOVERED_API_CACHE_TTL_SECONDS);
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
        _preferencesService.storeDiscoveredAPICache(_discoveredAPICache);
        _preferencesService.storeSavedProfileList(_savedProfileList);
        _preferencesService.storeSavedTokenList(_savedTokenList);
    }

    /**
     * Returns a discovered API from the cache.
     *
     * @param sanitizedBaseURI The sanitized base URI of the API.
     * @return A discovered API if a cached one was found. Null if none found.
     */
    @Nullable
    public DiscoveredAPI getCachedDiscoveredAPI(@NonNull String sanitizedBaseURI) {
        return _discoveredAPICache.get(sanitizedBaseURI);
    }

    /**
     * Caches a discovered API for future usage.
     *
     * @param sanitizedBaseURI The sanitized base URI of the API which was discovered.
     * @param discoveredAPI    The discovered API object to save.
     */
    public void cacheDiscoveredAPI(@NonNull String sanitizedBaseURI, @NonNull DiscoveredAPI discoveredAPI) {
        _discoveredAPICache.put(sanitizedBaseURI, discoveredAPI);
        _save();
    }

    /**
     * Returns a cached access token for an API.
     *
     * @param instance The instance to get the access token for.
     * @return The access token if found. Null if not found.
     */
    @Nullable
    public String getCachedAccessToken(@NonNull Instance instance) {
        for (SavedToken savedToken : _savedTokenList) {
            if (savedToken.getInstance().getSanitizedBaseURI().equals(instance.getSanitizedBaseURI())) {
                return savedToken.getAccessToken();
            } else if (instance.getAuthorizationType() == AuthorizationType.DISTRIBUTED || savedToken.getInstance().getAuthorizationType() == AuthorizationType.DISTRIBUTED) {
                return savedToken.getAccessToken();
            }
        }
        return null;
    }

    /**
     * Caches an access token for an API.
     *
     * @param instance    The VPN provider the token is stored for.
     * @param accessToken The access token to save.
     */
    public void cacheAccessToken(@NonNull Instance instance, @NonNull String accessToken) {
        // Remove all previous entries
        removeAccessTokens(instance);
        _savedTokenList.add(new SavedToken(instance, accessToken));
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
    public List<SavedToken> getSavedTokensForAuthorizationType(@AuthorizationType int authorizationType) {
        List<SavedToken> result = new ArrayList<>();
        for (SavedToken savedToken : _savedTokenList) {
            if (savedToken.getInstance().getAuthorizationType() == authorizationType) {
                result.add(savedToken);
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
    public void removeAccessTokens(@NonNull Instance instance) {
        Iterator<SavedToken> savedTokenIterator = _savedTokenList.iterator();
        while (savedTokenIterator.hasNext()) {
            SavedToken savedToken = savedTokenIterator.next();
            if (savedToken.getInstance().getSanitizedBaseURI().equals(instance.getBaseURI()) && instance.getAuthorizationType() == savedToken.getInstance().getAuthorizationType()) {
                savedTokenIterator.remove();
            }
        }
        _save();
        setChanged();
        notifyObservers(NOTIFICATION_TOKENS_CHANGED);
        clearChanged();
    }

    /**
     * Removes a discovered API based on the provider.
     *
     * @param sanitizedBaseURI The sanitized base URI of the provider.
     */
    public void removeDiscoveredAPI(@NonNull String sanitizedBaseURI) {
        _discoveredAPICache.remove(sanitizedBaseURI);
        _save();
    }

    /**
     * Returns the list of all saved tokens.
     *
     * @return The list of all saved access tokens and instances.
     */
    public List<SavedToken> getSavedTokenList() {
        return Collections.unmodifiableList(_savedTokenList);
    }

    /**
     * Removes the saved profiles for an instance.
     *
     * @param sanitizedBaseURI The sanitized base URI of an instance.
     */
    public void removeSavedProfilesForInstance(@NonNull String sanitizedBaseURI) {
        Iterator<SavedProfile> savedProfileIterator = _savedProfileList.iterator();
        while (savedProfileIterator.hasNext()) {
            SavedProfile savedProfile = savedProfileIterator.next();
            if (savedProfile.getInstance().getSanitizedBaseURI().equals(sanitizedBaseURI)) {
                savedProfileIterator.remove();
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
    public SavedToken getSavedToken(Instance instance) {
        // First we prioritize tokens which belong to the same instance
        for (SavedToken savedToken : _savedTokenList) {
            if (instance.getSanitizedBaseURI().equals(savedToken.getInstance().getSanitizedBaseURI())) {
                return savedToken;
            }
        }
        // Second pass: if distributed auth instance, any other instance with distributed auth is fine as well
        if (instance.getAuthorizationType() == AuthorizationType.DISTRIBUTED) {
            for (SavedToken savedToken : _savedTokenList) {
                if (savedToken.getInstance().getAuthorizationType() == AuthorizationType.DISTRIBUTED) {
                    return savedToken;
                }
            }
        }
        return null;
    }

    /**
     * Returns a saved key pair for a discovered API.
     *
     * @param discoveredAPI The discovered API.
     * @return The saved key pair if there was a previously generated one. Null if none created yet.
     */
    public SavedKeyPair getSavedKeyPairForAPI(DiscoveredAPI discoveredAPI) {
        for (SavedKeyPair savedKeyPair : _savedKeyPairList) {
            if (savedKeyPair.getApiBaseUri().equals(discoveredAPI.getApiBaseUri())) {
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
            if (current.getApiBaseUri().equals(savedKeyPair.getApiBaseUri())) {
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
}
