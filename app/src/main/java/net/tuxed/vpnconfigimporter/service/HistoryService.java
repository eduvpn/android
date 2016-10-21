package net.tuxed.vpnconfigimporter.service;

import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.SavedProfile;
import net.tuxed.vpnconfigimporter.entity.SavedToken;
import net.tuxed.vpnconfigimporter.utils.Log;
import net.tuxed.vpnconfigimporter.utils.TTLCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service which stores previously used access token and profile names.
 * This allows us to skip some steps, which will make the user experience more fluid.
 * Created by Daniel Zolnai on 2016-10-20.
 */
public class HistoryService {
    private static final String TAG = HistoryService.class.getName();

    private static final Long DISCOVERED_API_CACHE_TTL_SECONDS = 30 * 24 * 3600L; // 30 days

    private TTLCache<DiscoveredAPI> _discoveredAPICache;
    private List<SavedProfile> _savedProfileList;
    private List<SavedToken> _savedTokenList;

    private PreferencesService _preferencesService;

    public HistoryService(PreferencesService preferencesService) {
        _preferencesService = preferencesService;
        _load();
        _discoveredAPICache.purge();
    }

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
    }

    private void _save() {
        _preferencesService.storeDiscoveredAPICache(_discoveredAPICache);
        _preferencesService.storeSavedProfileList(_savedProfileList);
        _preferencesService.storeSavedTokenList(_savedTokenList);
    }

    public DiscoveredAPI getCachedDiscoveredAPI(String normalizedBaseUri) {
        return _discoveredAPICache.get(normalizedBaseUri);
    }

    public void cacheDiscoveredAPI(String normalizedBaseUri, DiscoveredAPI discoveredAPI) {
        _discoveredAPICache.put(normalizedBaseUri, discoveredAPI);
        _save();
    }

    public String getCachedAccessToken(String normalizedBaseUri) {
        for (SavedToken savedToken : _savedTokenList) {
            if (savedToken.getBaseUri().equals(normalizedBaseUri)) {
                return savedToken.getAccessToken();
            }
        }
        return null;
    }

    public void cacheToken(String normalizedBaseUri, String accessToken) {
        _savedTokenList.add(new SavedToken(normalizedBaseUri, accessToken));
        _save();
    }

    public List<SavedProfile> getSavedProfileList() {
        return Collections.unmodifiableList(_savedProfileList);
    }

    public void cacheSavedProfile(SavedProfile savedProfile) {
        _savedProfileList.add(savedProfile);
        _save();
    }

    public SavedProfile getCachedSavedProfile(String sanitizedBaseUri, String profileId) {
        for (SavedProfile savedProfile : _savedProfileList) {
            if (savedProfile.getInstance().getSanitizedBaseUri().equals(sanitizedBaseUri) &&
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
    public void removeSavedProfile(SavedProfile savedProfile) {
        _savedProfileList.remove(savedProfile);
        _save();
    }
}
