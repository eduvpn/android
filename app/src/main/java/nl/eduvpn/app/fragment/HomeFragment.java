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

package nl.eduvpn.app.fragment;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import net.openid.appauth.AuthState;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observer;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.blinkt.openvpn.VpnProfile;
import nl.eduvpn.app.BuildConfig;
import nl.eduvpn.app.Constants;
import nl.eduvpn.app.EduVPNApplication;
import nl.eduvpn.app.MainActivity;
import nl.eduvpn.app.R;
import nl.eduvpn.app.adapter.ProfileAdapter;
import nl.eduvpn.app.base.BaseFragment;
import nl.eduvpn.app.databinding.FragmentHomeBinding;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.KeyPair;
import nl.eduvpn.app.entity.Profile;
import nl.eduvpn.app.entity.SavedAuthState;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.entity.SavedProfile;
import nl.eduvpn.app.service.APIService;
import nl.eduvpn.app.service.ConfigurationService;
import nl.eduvpn.app.service.ConnectionService;
import nl.eduvpn.app.service.HistoryService;
import nl.eduvpn.app.service.PreferencesService;
import nl.eduvpn.app.service.SerializerService;
import nl.eduvpn.app.service.VPNService;
import nl.eduvpn.app.utils.ErrorDialog;
import nl.eduvpn.app.utils.FormattingUtils;
import nl.eduvpn.app.utils.ItemClickSupport;
import nl.eduvpn.app.utils.Log;
import nl.eduvpn.app.utils.SwipeToDeleteAnimator;
import nl.eduvpn.app.utils.SwipeToDeleteHelper;

/**
 * Fragment which is displayed when the app start.
 * Created by Daniel Zolnai on 2016-10-20.
 */
public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    private static final String TAG = HomeFragment.class.getName();

    public static final String KEY_SKIP_FIRST_UPDATE = "key_skip_first_update";

    @Inject
    protected HistoryService _historyService;

    @Inject
    protected VPNService _vpnService;

    @Inject
    protected PreferencesService _preferencesService;

    @Inject
    protected APIService _apiService;

    @Inject
    protected SerializerService _serializerService;

    @Inject
    protected ConnectionService _connectionService;

    @Inject
    protected ConfigurationService _configurationService;

    private int _pendingInstanceCount;
    private List<Instance> _problematicInstances;
    private Observer _newServerObserver;

    private Dialog _currentDialog;

    public static HomeFragment newInstance(boolean skipFirstUpdate) {
        HomeFragment homeFragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putBoolean(HomeFragment.KEY_SKIP_FIRST_UPDATE, skipFirstUpdate);
        homeFragment.setArguments(args);
        return homeFragment;
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_home;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EduVPNApplication.get(view.getContext()).component().inject(this);

        // Basic setup of the lists
        binding.secureInternetList.setHasFixedSize(true);
        binding.instituteAccessList.setHasFixedSize(true);
        binding.secureInternetList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        binding.instituteAccessList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));

        // Add the adapters
        ProfileAdapter instituteAccessAdapter = new ProfileAdapter(_historyService, null);
        binding.instituteAccessList.setAdapter(instituteAccessAdapter);

        ProfileAdapter secureInternetAdapter = new ProfileAdapter(_historyService, null);
        binding.secureInternetList.setAdapter(secureInternetAdapter);

        binding.addProvider.setOnClickListener(v -> onAddProviderClicked());

        // Swipe to delete
        ItemTouchHelper instituteSwipeHelper = new ItemTouchHelper(new SwipeToDeleteHelper(getContext()));
        instituteSwipeHelper.attachToRecyclerView(binding.instituteAccessList);
        binding.instituteAccessList.addItemDecoration(new SwipeToDeleteAnimator(getContext()));
        ItemTouchHelper secureInternetSwipeHelper = new ItemTouchHelper(new SwipeToDeleteHelper(getContext()));
        secureInternetSwipeHelper.attachToRecyclerView(binding.secureInternetList);
        binding.secureInternetList.addItemDecoration(new SwipeToDeleteAnimator(getContext()));

        // Add click listeners
        ItemClickSupport.OnItemClickListener clickListener = (recyclerView, position, v) -> _onItemClicked(recyclerView, position);
        ItemClickSupport.addTo(binding.instituteAccessList).setOnItemClickListener(clickListener);
        ItemClickSupport.addTo(binding.secureInternetList).setOnItemClickListener(clickListener);
        ItemClickSupport.OnItemLongClickListener longClickListener = (recyclerView, position, v) -> _onItemLongClicked(recyclerView, position);
        ItemClickSupport.addTo(binding.instituteAccessList).setOnItemLongClickListener(longClickListener);
        ItemClickSupport.addTo(binding.secureInternetList).setOnItemLongClickListener(longClickListener);

        // Fill the lists with data
        boolean skipUpdate = getArguments() != null && getArguments().containsKey(KEY_SKIP_FIRST_UPDATE) && getArguments().getBoolean(KEY_SKIP_FIRST_UPDATE);
        if (!skipUpdate) {
            _updateLists();
        }
        // Add listener on the history service, so we get notified if there is a new server available
        _historyService.addObserver(_newServerObserver = (observable, o) -> {
            if (o instanceof Integer) {
                Integer notificationType = (Integer)o;
                if (HistoryService.NOTIFICATION_PROFILES_CHANGED.equals(notificationType) || HistoryService.NOTIFICATION_TOKENS_CHANGED.equals(notificationType)) {
                    _updateLists();
                }
            } else {
                Log.e(TAG, "Unexpected notification type! Live reload might not be working correctly.");
            }
        });
    }

    private boolean _onItemLongClicked(RecyclerView recyclerView, int position) {
        // On long click we show the full name in a toast
        // Is useful when the names don't fit too well.
        ProfileAdapter adapter = (ProfileAdapter)recyclerView.getAdapter();
        if (adapter == null) {
            return false;
        }
        if (adapter.isPendingRemoval(position)) {
            return true;
        }
        Pair<Instance, Profile> instanceProfilePair = adapter.getItem(position);
        Toast.makeText(
                getContext(),
                FormattingUtils.formatProfileName(getContext(), instanceProfilePair.first, instanceProfilePair.second),
                Toast.LENGTH_SHORT).show();
        return true;
    }

    private void _onItemClicked(RecyclerView recyclerView, int position) {
        ProfileAdapter adapter = (ProfileAdapter)recyclerView.getAdapter();
        if (adapter.isPendingRemoval(position)) {
            return;
        }
        Pair<Instance, Profile> instanceProfilePair = adapter.getItem(position);
        // We surely have a discovered API and access token, since we just loaded the list with them
        Instance instance = instanceProfilePair.first;
        Profile profile = instanceProfilePair.second;
        DiscoveredAPI discoveredAPI = _historyService.getCachedDiscoveredAPI(instance.getSanitizedBaseURI());
        AuthState authState = _historyService.getCachedAuthState(instance);
        if (discoveredAPI == null || authState == null) {
            ErrorDialog.show(getContext(), R.string.error_dialog_title, R.string.cant_connect_application_state_missing);
            return;
        }
        _preferencesService.currentInstance(instance);
        _preferencesService.storeCurrentDiscoveredAPI(discoveredAPI);
        _preferencesService.storeCurrentProfile(profile);
        _preferencesService.storeCurrentAuthState(authState);
        // Always download a new profile.
        // Just to be sure,
        _downloadKeyPairIfNeeded(instance, discoveredAPI, profile, authState);
    }

    private void _updateLists() {
        // Fill with data
        List<SavedAuthState> savedInstituteAccessTokens = _historyService.getSavedTokensForAuthorizationType(AuthorizationType.LOCAL);
        List<SavedAuthState> savedSecureInternetTokens = _historyService.getSavedTokensForAuthorizationType(AuthorizationType.DISTRIBUTED);
        // Secure internet tokens are valid for all other instances.
        savedSecureInternetTokens = enhanceSecureInternetTokensList(savedSecureInternetTokens);
        if (savedInstituteAccessTokens.isEmpty() && savedSecureInternetTokens.isEmpty()) {
            // No saved tokens
            binding.loadingBar.setVisibility(View.GONE);
            binding.noProvidersYet.setVisibility(View.VISIBLE);
            binding.instituteAccessContainer.setVisibility(View.GONE);
            binding.secureInternetContainer.setVisibility(View.GONE);
        } else {

            binding.loadingBar.setVisibility(View.VISIBLE);
            binding.noProvidersYet.setVisibility(View.GONE);
            // There are some saved institute access tokens
            if (!savedInstituteAccessTokens.isEmpty()) {
                _fillList((ProfileAdapter)binding.instituteAccessList.getAdapter(), savedInstituteAccessTokens);
                binding.instituteAccessContainer.setVisibility(View.VISIBLE);
            } else {
                binding.instituteAccessContainer.setVisibility(View.GONE);
            }

            // There are some saved secure internet tokens
            if (!savedSecureInternetTokens.isEmpty()) {
                _fillList((ProfileAdapter)binding.secureInternetList.getAdapter(), savedSecureInternetTokens);
                binding.secureInternetContainer.setVisibility(View.VISIBLE);
            } else {
                binding.secureInternetContainer.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Creates a list with all distributed auth instances, using the same token.
     *
     * @return The list of all distributed auth instances.
     */
    private List<SavedAuthState> enhanceSecureInternetTokensList(List<SavedAuthState> savedSecureInternetTokens) {
        if (savedSecureInternetTokens == null) {
            return null;
        }
        if (savedSecureInternetTokens.size() == 0) {
            return savedSecureInternetTokens;
        }
        AuthState authState = savedSecureInternetTokens.get(0).getAuthState();
        List<Instance> instanceList = _configurationService.getSecureInternetList();
        List<SavedAuthState> result = new ArrayList<>();
        for (Instance instance : instanceList) {
            result.add(new SavedAuthState(instance, authState));
        }
        return result;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (_currentDialog != null) {
            _currentDialog.dismiss();
            _currentDialog = null;
        }
        if (_newServerObserver != null) {
            _historyService.deleteObserver(_newServerObserver);
        }
    }


    /**
     * Starts fetching the list of profiles to be displayed.
     * This will be done from multiple APIs and loaded asynchronously.
     * While the list is still filling, a loading indicator is shown. When all resources were downloaded,
     * indicator will be hidden.
     *
     * @param adapter                  The adapter to show the profiles in.
     * @param instanceAccessTokenPairs Each instance & access token pair.
     */
    private void _fillList(final ProfileAdapter adapter, List<SavedAuthState> instanceAccessTokenPairs) {
        _pendingInstanceCount = instanceAccessTokenPairs.size();
        _problematicInstances = new ArrayList<>();
        for (SavedAuthState savedAuthState : instanceAccessTokenPairs) {
            final Instance instance = savedAuthState.getInstance();
            final AuthState authState = savedAuthState.getAuthState();
            DiscoveredAPI discoveredAPI = _historyService.getCachedDiscoveredAPI(instance.getSanitizedBaseURI());
            if (discoveredAPI != null) {
                // We got everything, fetch the available profiles.
                _fetchProfileList(adapter, instance, discoveredAPI, authState);
            } else {
                _apiService.getJSON(instance.getSanitizedBaseURI() + Constants.API_DISCOVERY_POSTFIX,
                        authState,
                        new APIService.Callback<JSONObject>() {
                            @Override
                            public void onSuccess(JSONObject result) {
                                try {
                                    DiscoveredAPI discoveredAPI = _serializerService.deserializeDiscoveredAPI(result);
                                    // Cache the result
                                    _historyService.cacheDiscoveredAPI(instance.getSanitizedBaseURI(), discoveredAPI);
                                    _fetchProfileList(adapter, instance, discoveredAPI, authState);
                                } catch (SerializerService.UnknownFormatException ex) {
                                    Log.e(TAG, "Error parsing discovered API!", ex);
                                    _problematicInstances.add(instance);
                                    _checkLoadingFinished(adapter);
                                }
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e(TAG, "Error while fetching discovered API: " + errorMessage);
                                _problematicInstances.add(instance);
                                _checkLoadingFinished(adapter);
                            }
                        });
            }
        }
    }

    /**
     * Starts downloading the list of profiles for a single VPN provider.
     *
     * @param adapter       The adapter to download the data into.
     * @param instance      The VPN provider instance.
     * @param discoveredAPI The discovered API containing the URLs.
     * @param authState     The access and refresh token for the API.
     */
    private void _fetchProfileList(@NonNull final ProfileAdapter adapter, @NonNull final Instance instance,
                                   @NonNull DiscoveredAPI discoveredAPI, @NonNull AuthState authState) {
        _apiService.getJSON(discoveredAPI.getProfileListEndpoint(), authState, new APIService.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    List<Profile> profiles = _serializerService.deserializeProfileList(result);
                    List<Pair<Instance, Profile>> newItems = new ArrayList<>();
                    for (Profile profile : profiles) {
                        newItems.add(new Pair<>(instance, profile));
                    }
                    adapter.addItemsIfNotAdded(newItems);
                } catch (SerializerService.UnknownFormatException ex) {
                    _problematicInstances.add(instance);
                    Log.e(TAG, "Error parsing profile list.", ex);
                }
                _checkLoadingFinished(adapter);
            }

            @Override
            public void onError(String errorMessage) {
                _problematicInstances.add(instance);
                Log.e(TAG, "Error fetching profile list: " + errorMessage);
                _checkLoadingFinished(adapter);
            }
        });
    }

    /**
     * Checks if the loading has finished.
     * If yes, it hides the loading animation.
     * If there were any errors, it will display a warning bar as well.
     *
     * @param adapter The adapter which the items are being loaded into.
     */
    private synchronized void _checkLoadingFinished(final ProfileAdapter adapter) {
        _pendingInstanceCount--;
        if (_pendingInstanceCount <= 0 && _problematicInstances.size() == 0) {
            if (binding.loadingBar == null) {
                Log.d(TAG, "Layout has been destroyed already.");
                return;
            }
            float startHeight = binding.loadingBar.getHeight();
            ValueAnimator animator = ValueAnimator.ofFloat(startHeight, 0);
            animator.setDuration(600);
            animator.addUpdateListener(animation -> {
                float fraction = animation.getAnimatedFraction();
                float alpha = 1f - fraction;
                float height = (Float)animation.getAnimatedValue();
                if (binding.loadingBar != null) {
                    binding.loadingBar.setAlpha(alpha);
                    binding.loadingBar.getLayoutParams().height = (int)height;
                    binding.loadingBar.requestLayout();
                }
            });
            animator.start();
        } else if (_pendingInstanceCount <= 0) {
            if (binding.displayText == null) {
                Log.d(TAG, "Layout has been destroyed already.");
                return;
            }
            // There are some warnings
            binding.displayText.setText(R.string.could_not_fetch_all_profiles);
            binding.warningIcon.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
            binding.loadingBar.setOnClickListener(v -> {
                // Display a dialog with all the warnings
                _currentDialog = ErrorDialog.show(getContext(),
                        getString(R.string.warnings_list),
                        getString(R.string.instance_access_warning_message),
                        new ErrorDialog.InstanceWarningHandler() {
                            @Override
                            public List<Instance> getInstances() {
                                return _problematicInstances;
                            }

                            @Override
                            public void retryInstance(Instance instance) {
                                binding.warningIcon.setVisibility(View.GONE);
                                binding.progressBar.setVisibility(View.VISIBLE);
                                binding.displayText.setText(R.string.loading_available_profiles);
                                SavedAuthState savedAuthState = _historyService.getSavedToken(instance);
                                if (savedAuthState == null) {
                                    // Should never happen
                                    _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title, R.string.data_removed);
                                } else {
                                    // Retry
                                    _problematicInstances.remove(instance);
                                    _fillList(adapter, Collections.singletonList(savedAuthState));
                                }
                            }

                            @Override
                            public void loginInstance(final Instance instance) {
                                // Find the auth state for the instance and then retry
                                AuthState authState = _historyService.getCachedAuthState(instance);
                                _apiService.getJSON(instance.getSanitizedBaseURI() + Constants.API_DISCOVERY_POSTFIX,
                                        authState,
                                        new APIService.Callback<JSONObject>() {
                                            @Override
                                            public void onSuccess(JSONObject result) {
                                                try {
                                                    DiscoveredAPI discoveredAPI = _serializerService.deserializeDiscoveredAPI(result);
                                                    // Cache the result
                                                    _historyService.cacheDiscoveredAPI(instance.getSanitizedBaseURI(), discoveredAPI);
                                                    _problematicInstances.remove(instance);
                                                    Activity activity = getActivity();
                                                    if (activity != null && !activity.isFinishing()) {
                                                        _connectionService.initiateConnection(getActivity(), instance, discoveredAPI);
                                                    }
                                                } catch (SerializerService.UnknownFormatException ex) {
                                                    Log.e(TAG, "Error parsing discovered API!", ex);
                                                    _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title, R.string.provider_incorrect_format);
                                                }
                                            }

                                            @Override
                                            public void onError(String errorMessage) {
                                                Log.e(TAG, "Error while fetching discovered API: " + errorMessage);
                                                DiscoveredAPI discoveredAPI = _historyService.getCachedDiscoveredAPI(instance.getSanitizedBaseURI());
                                                Activity activity = getActivity();
                                                if (discoveredAPI != null && activity != null && !activity.isFinishing()) {
                                                    _connectionService.initiateConnection(activity, instance, discoveredAPI);
                                                } else {
                                                    _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title, R.string.provider_not_found_retry);
                                                }
                                            }
                                        });
                            }

                            @Override
                            public void removeInstance(Instance instance) {
                                _historyService.removeAllDataForInstance(instance);
                                _problematicInstances.remove(instance);
                                getActivity().runOnUiThread(() -> _checkLoadingFinished(adapter));
                            }
                        });
            });
        }
    }

    /**
     * Downloads the key pair if no cached one found. After that it downloads the profile and connects to it.
     *
     * @param instance      The VPN provider.
     * @param discoveredAPI The discovered API.
     * @param profile       The profile to download.
     */
    private void _downloadKeyPairIfNeeded(@NonNull final Instance instance, @NonNull final DiscoveredAPI discoveredAPI,
                                          @NonNull final Profile profile, @NonNull final AuthState authState) {
        // First we create a keypair, if there is no saved one yet.
        SavedKeyPair savedKeyPair = _historyService.getSavedKeyPairForInstance(instance);
        int dialogMessageRes = savedKeyPair != null ? R.string.vpn_profile_checking_certificate : R.string.vpn_profile_creating_keypair;
        ProgressDialog progressDialog = ProgressDialog.show(getContext(),
                getString(R.string.progress_dialog_title),
                getString(dialogMessageRes),
                true,
                false);
        _currentDialog = progressDialog;
        if (savedKeyPair != null) {
            _checkCertificateValidity(instance, discoveredAPI, savedKeyPair, profile, authState, progressDialog);
            return;
        }

        String requestData = "display_name=eduVPN";
        try {
            requestData = "display_name=" + URLEncoder.encode(BuildConfig.CERTIFICATE_DISPLAY_NAME, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // unable to encode the display name, use default
        }

        String createKeyPairEndpoint = discoveredAPI.getCreateKeyPairEndpoint();
        _apiService.postResource(createKeyPairEndpoint, requestData, authState, new APIService.Callback<String>() {

            @Override
            public void onSuccess(String keyPairJson) {
                try {
                    KeyPair keyPair = _serializerService.deserializeKeyPair(new JSONObject(keyPairJson));
                    Log.i(TAG, "Created key pair, is it successful: " + keyPair.isOK());
                    // Save it for later
                    SavedKeyPair savedKeyPair = new SavedKeyPair(instance, keyPair);
                    _historyService.storeSavedKeyPair(savedKeyPair);
                    _downloadProfileWithKeyPair(instance, discoveredAPI, savedKeyPair, profile, authState, progressDialog);
                } catch (Exception ex) {
                    progressDialog.dismiss();
                    if (getActivity() != null) {
                        _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title, getString(R.string.error_parsing_keypair, ex.getMessage()));
                    }
                    Log.e(TAG, "Unable to parse keypair data", ex);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title, getString(R.string.error_creating_keypair, errorMessage));
                Log.e(TAG, "Error creating keypair: " + errorMessage);
            }
        });
    }

    /**
     * Now that we have the key pair, we can download the profile.
     *
     * @param instance      The API instance definition.
     * @param discoveredAPI The discovered API URLs.
     * @param savedKeyPair  The private key and certificate used to generate the profile.
     * @param profile       The profile to create.
     * @param dialog        Loading dialog which should be dismissed when finished.
     */
    private void _downloadProfileWithKeyPair(final Instance instance, DiscoveredAPI discoveredAPI,
                                             final SavedKeyPair savedKeyPair, final Profile profile,
                                             final AuthState authState,
                                             final ProgressDialog dialog) {
        dialog.setMessage(getString(R.string.vpn_profile_download_message));
        String requestData = "?profile_id=" + profile.getProfileId();
        _apiService.getString(discoveredAPI.getProfileConfigEndpoint() + requestData, authState, new APIService.Callback<String>() {
            @Override
            public void onSuccess(String vpnConfig) {
                // The downloaded profile misses the <cert> and <key> fields. We will insert that via the saved key pair.
                String configName = FormattingUtils.formatProfileName(getContext(), instance, profile);
                VpnProfile vpnProfile = _vpnService.importConfig(vpnConfig, configName, savedKeyPair);
                if (vpnProfile != null && getActivity() != null) {
                    // Cache the profile
                    SavedProfile savedProfile = new SavedProfile(instance, profile, vpnProfile.getUUIDString());
                    _historyService.cacheSavedProfile(savedProfile);
                    // Connect with the profile
                    dialog.dismiss();
                    _vpnService.connect(getActivity(), vpnProfile);
                    ((MainActivity)getActivity()).openFragment(new ConnectionStatusFragment(), false);
                } else {
                    dialog.dismiss();
                    _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title, R.string.error_importing_profile);
                }
            }

            @Override
            public void onError(String errorMessage) {
                dialog.dismiss();
                _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title, getString(R.string.error_fetching_profile, errorMessage));
                Log.e(TAG, "Error fetching profile: " + errorMessage);
            }
        });
    }

    private void _checkCertificateValidity(Instance instance, DiscoveredAPI discoveredAPI, SavedKeyPair savedKeyPair, Profile profile, AuthState authState, ProgressDialog dialog) {
        String commonName = savedKeyPair.getKeyPair().getCertificateCommonName();
        if (commonName == null) {
            // Unable to check, better download it again.
            _historyService.removeSavedKeyPairs(instance);
            // Try downloading it again.
            dialog.dismiss();
            _downloadKeyPairIfNeeded(instance, discoveredAPI, profile, authState);
            Log.w(TAG, "Could not check if certificate is valid. Downloading again.");
        }
        _apiService.getJSON(discoveredAPI.getCheckCertificateEndpoint(commonName), authState, new APIService.Callback<JSONObject>() {

            @Override
            public void onSuccess(JSONObject result) {
                try {
                    Boolean isValid = result.getJSONObject("check_certificate").getJSONObject("data").getBoolean("is_valid");
                    if (isValid) {
                        Log.i(TAG, "Certificate appears to be valid.");
                        _downloadProfileWithKeyPair(instance, discoveredAPI, savedKeyPair, profile, authState, dialog);
                    } else {
                        dialog.dismiss();
                        String reason = result.getJSONObject("check_certificate").getJSONObject("data").getString("reason");
                        if ("user_disabled".equals(reason) || "certificate_disabled".equals(reason)) {
                            int errorStringId = R.string.error_certificate_disabled;
                            if ("user_disabled".equals(reason)) {
                                errorStringId = R.string.error_user_disabled;
                            }
                            _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title_invalid_certificate, getString(errorStringId));
                        } else {
                            // Remove stored keypair.
                            _historyService.removeSavedKeyPairs(instance);
                            Log.i(TAG, "Certificate is invalid. Fetching new one. Reason: " + reason);
                            // Try downloading it again.
                            _downloadKeyPairIfNeeded(instance, discoveredAPI, profile, authState);
                        }

                    }
                } catch (Exception ex) {
                    dialog.dismiss();
                    _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title, getString(R.string.error_parsing_certificate));
                    Log.e(TAG, "Unexpected certificate call response!", ex);
                }
            }

            @Override
            public void onError(String errorMessage) {
                dialog.dismiss();
                if (errorMessage != null && (APIService.USER_NOT_AUTHORIZED_ERROR.equals(errorMessage) ||
                        errorMessage.contains("invalid_grant"))) {
                    // Display a dialog with all the warnings
                    _currentDialog = ErrorDialog.show(getContext(),
                            getString(R.string.warnings_list),
                            getString(R.string.instance_access_warning_message),
                            new ErrorDialog.InstanceWarningHandler() {
                                @Override
                                public List<Instance> getInstances() {
                                    return Collections.singletonList(instance);
                                }

                                @Override
                                public void retryInstance(Instance instance) {
                                    SavedAuthState savedAuthState = _historyService.getSavedToken(instance);
                                    if (savedAuthState == null) {
                                        // Should never happen
                                        _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title, R.string.data_removed);
                                    } else {
                                        int dialogMessageRes = R.string.vpn_profile_checking_certificate;
                                        final ProgressDialog dialog = ProgressDialog.show(getContext(),
                                                getString(R.string.progress_dialog_title),
                                                getString(dialogMessageRes),
                                                true,
                                                false);
                                        dialog.show();
                                        _currentDialog = dialog;
                                        _checkCertificateValidity(instance, discoveredAPI, savedKeyPair, profile, savedAuthState.getAuthState(), dialog);
                                    }
                                }

                                @Override
                                public void loginInstance(final Instance instance) {
                                    // Find the auth state for the instance and then retry
                                    AuthState authState = _historyService.getCachedAuthState(instance);
                                    _apiService.getJSON(instance.getSanitizedBaseURI() + Constants.API_DISCOVERY_POSTFIX,
                                            authState,
                                            new APIService.Callback<JSONObject>() {
                                                @Override
                                                public void onSuccess(JSONObject result) {
                                                    try {
                                                        DiscoveredAPI discoveredAPI = _serializerService.deserializeDiscoveredAPI(result);
                                                        // Cache the result
                                                        _historyService.cacheDiscoveredAPI(instance.getSanitizedBaseURI(), discoveredAPI);
                                                        _connectionService.initiateConnection(getActivity(), instance, discoveredAPI);
                                                    } catch (SerializerService.UnknownFormatException ex) {
                                                        Log.e(TAG, "Error parsing discovered API!", ex);
                                                        _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title, R.string.provider_incorrect_format);
                                                    }
                                                }

                                                @Override
                                                public void onError(String errorMessage) {
                                                    Log.e(TAG, "Error while fetching discovered API: " + errorMessage);
                                                    DiscoveredAPI discoveredAPI = _historyService.getCachedDiscoveredAPI(instance.getSanitizedBaseURI());
                                                    Activity activity = getActivity();
                                                    if (discoveredAPI != null && activity != null && !activity.isFinishing()) {
                                                        _connectionService.initiateConnection(activity, instance, discoveredAPI);
                                                    } else {
                                                        _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title, R.string.provider_not_found_retry);
                                                    }
                                                }
                                            });
                                }

                                @Override
                                public void removeInstance(Instance instance) {
                                    _historyService.removeAllDataForInstance(instance);
                                }
                            });
                } else {
                    _currentDialog = ErrorDialog.show(getContext(), R.string.error_dialog_title, getString(R.string.error_checking_certificate));
                    Log.e(TAG, "Error checking certificate: " + errorMessage);
                }

            }
        });
    }

    /**
     * Called when the user clicks on the 'Add Provider' button.
     */
    protected void onAddProviderClicked() {
        if (BuildConfig.API_DISCOVERY_ENABLED) {
            /* for "basic" we ask for the type of the provider to add */
            ((MainActivity)getActivity()).openFragment(new TypeSelectorFragment(), true);
        } else {
            /* for "home", i.e. Let's Connect! we immediately ask for the domain */
            ((MainActivity)getActivity()).openFragment(new CustomProviderFragment(), true);
        }

    }
}
