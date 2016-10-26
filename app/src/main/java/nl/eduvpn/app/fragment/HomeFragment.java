package nl.eduvpn.app.fragment;

import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import nl.eduvpn.app.Constants;
import nl.eduvpn.app.EduVPNApplication;
import nl.eduvpn.app.MainActivity;
import nl.eduvpn.app.R;
import nl.eduvpn.app.adapter.ProfileAdapter;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.Profile;
import nl.eduvpn.app.entity.SavedProfile;
import nl.eduvpn.app.entity.SavedToken;
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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import de.blinkt.openvpn.VpnProfile;
import nl.eduvpn.app.utils.SwipeToDeleteAnimator;
import nl.eduvpn.app.utils.SwipeToDeleteHelper;

/**
 * Fragment which is displayed when the app start.
 * Created by Daniel Zolnai on 2016-10-20.
 */
public class HomeFragment extends Fragment {

    private static final String TAG = HomeFragment.class.getName();

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

    @BindView(R.id.profileList)
    protected RecyclerView _profileList;

    @BindView(R.id.noProvidersYet)
    protected TextView _noProvidersYet;

    @BindView(R.id.loadingBar)
    protected ViewGroup _loadingBar;

    @BindView(R.id.displayText)
    protected TextView _displayText;

    @BindView(R.id.warningIcon)
    protected ImageView _warningIcon;

    @BindView(R.id.progressBar)
    protected View _progressBar;

    private Unbinder _unbinder;

    private int _pendingInstanceCount;
    private List<String> _warnings;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        _unbinder = ButterKnife.bind(this, view);
        EduVPNApplication.get(view.getContext()).component().inject(this);
        _profileList.setHasFixedSize(true);
        _profileList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        final List<SavedToken> savedTokenList = _historyService.getSavedTokenList();
        if (savedTokenList.size() == 0) {
            _loadingBar.setVisibility(View.GONE);
            _noProvidersYet.setVisibility(View.VISIBLE);
            _profileList.setVisibility(View.GONE);
        } else {
            _loadingBar.setVisibility(View.VISIBLE);
            _noProvidersYet.setVisibility(View.GONE);
            _profileList.setVisibility(View.VISIBLE);
            ProfileAdapter adapter = new ProfileAdapter(_historyService, null);
            _profileList.setAdapter(adapter);
            ItemTouchHelper swipeHelper = new ItemTouchHelper(new SwipeToDeleteHelper(getContext()));
            swipeHelper.attachToRecyclerView(_profileList);
            _profileList.addItemDecoration(new SwipeToDeleteAnimator(getContext()));
            _fillList(adapter, savedTokenList);
        }

        ItemClickSupport.addTo(_profileList).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                ProfileAdapter adapter = (ProfileAdapter)recyclerView.getAdapter();
                if (adapter.isPendingRemoval(position)) {
                    return;
                }
                Pair<Instance, Profile> instanceProfilePair = adapter.getItem(position);
                // We surely have a discovered API and access token, since we just loaded the list with them
                Instance instance = instanceProfilePair.first;
                Profile profile = instanceProfilePair.second;
                DiscoveredAPI discoveredAPI = _historyService.getCachedDiscoveredAPI(instance.getSanitizedBaseURI());
                String accessToken = _historyService.getCachedAccessToken(instance.getSanitizedBaseURI());
                if (discoveredAPI == null || accessToken == null) {
                    ErrorDialog.show(getContext(), R.string.error_dialog_title, R.string.cant_connect_application_state_missing);
                    return;
                }
                _preferencesService.currentInstance(instance);
                _preferencesService.currentDiscoveredAPI(discoveredAPI);
                _preferencesService.currentProfile(profile);
                _connectionService.setAccessToken(accessToken);
                // In case we already have a downloaded profile, connect to it right away.
                SavedProfile savedProfile = _historyService.getCachedSavedProfile(instance.getSanitizedBaseURI(), profile.getProfileId());
                if (savedProfile != null) {
                    String profileUUID = savedProfile.getProfileUUID();
                    VpnProfile vpnProfile = _vpnService.getProfileWithUUID(profileUUID);
                    if (vpnProfile != null) {
                        // Profile found, connecting
                        _vpnService.connect(getActivity(), vpnProfile);
                        ((MainActivity)getActivity()).openFragment(new ConnectionStatusFragment(), false);
                        return;
                    } else {
                        Log.e(TAG, "Profile is not saved even it was marked as one!");
                        _historyService.removeSavedProfile(savedProfile);
                        // Continue with downloading the profile
                    }
                }
                // Ok so we don't have a downloaded profile, we need to download one
                _downloadProfileAndConnect(instance, discoveredAPI, profile);
            }
        });
        ItemClickSupport.addTo(_profileList).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
                // On long click we show the full name in a toast
                // Is useful when the names don't fit too well.
                ProfileAdapter adapter = (ProfileAdapter)recyclerView.getAdapter();
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
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _unbinder.unbind();
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
    private void _fillList(final ProfileAdapter adapter, List<SavedToken> instanceAccessTokenPairs) {
        _pendingInstanceCount = instanceAccessTokenPairs.size();
        _warnings = new ArrayList<>();
        for (SavedToken savedToken : instanceAccessTokenPairs) {
            final Instance instance = savedToken.getInstance();
            final String accessToken = savedToken.getAccessToken();
            DiscoveredAPI discoveredAPI = _historyService.getCachedDiscoveredAPI(instance.getSanitizedBaseURI());
            if (discoveredAPI != null) {
                // We got everything, fetch the available profiles.
                _fetchProfileList(adapter, instance, discoveredAPI, accessToken);
            } else {
                _apiService.getJSON(instance.getSanitizedBaseURI() + Constants.API_DISCOVERY_POSTFIX,
                        false,
                        new APIService.Callback<JSONObject>() {
                            @Override
                            public void onSuccess(JSONObject result) {
                                try {
                                    DiscoveredAPI discoveredAPI = _serializerService.deserializeDiscoveredAPI(result);
                                    // Cache the result
                                    _historyService.cacheDiscoveredAPI(instance.getSanitizedBaseURI(), discoveredAPI);
                                    _fetchProfileList(adapter, instance, discoveredAPI, accessToken);
                                } catch (SerializerService.UnknownFormatException ex) {
                                    Log.e(TAG, "Error parsing discovered API!", ex);
                                    _warnings.add(getString(R.string.api_discovery_failed, instance.getDisplayName()));
                                    _checkLoadingFinished();
                                }
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e(TAG, "Error while fetching discovered API: " + errorMessage);
                                _warnings.add(getString(R.string.api_discovery_failed, instance.getDisplayName()));
                                _checkLoadingFinished();
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
     * @param accessToken   The access token for the API.
     */
    private void _fetchProfileList(@NonNull final ProfileAdapter adapter, @NonNull final Instance instance,
                                   @NonNull DiscoveredAPI discoveredAPI, @NonNull String accessToken) {
        _connectionService.setAccessToken(accessToken);
        _apiService.getJSON(discoveredAPI.getProfileListAPI(), true, new APIService.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    List<Profile> profiles = _serializerService.deserializeProfileList(result);
                    List<Pair<Instance, Profile>> newItems = new ArrayList<>();
                    for (Profile profile : profiles) {
                        newItems.add(new Pair<>(instance, profile));
                    }
                    adapter.addItems(newItems);
                } catch (SerializerService.UnknownFormatException ex) {
                    _warnings.add(getString(R.string.unable_to_fetch_profiles, instance.getDisplayName()));
                    Log.e(TAG, "Error parsing profile list.", ex);
                }
                _checkLoadingFinished();
            }

            @Override
            public void onError(String errorMessage) {
                _warnings.add(getString(R.string.unable_to_fetch_profiles, instance.getDisplayName()));
                Log.e(TAG, "Error fetching profile list: " + errorMessage);
                if (APIService.USER_NOT_AUTHORIZED_ERROR.equals(errorMessage)) {
                    // Token is not valid anymore.
                    _historyService.removeAccessTokens(instance.getSanitizedBaseURI());
                    _historyService.removeDiscoveredAPI(instance.getSanitizedBaseURI());
                    Log.e(TAG, "API returned unauthorized error, removed cache for provider.");
                }
                _checkLoadingFinished();
            }
        });
    }

    /**
     * Checks if the loading has finished.
     * If yes, it hides the loading animation.
     * If there were any errors, it will display a warning bar as well.
     */
    private synchronized void _checkLoadingFinished() {
        _pendingInstanceCount--;
        if (_pendingInstanceCount == 0 && _warnings.size() == 0) {
            if (_loadingBar == null) {
                Log.w(TAG, "Layout has been destroyed already.");
                return;
            }
            float startHeight = _loadingBar.getHeight();
            ValueAnimator animator = ValueAnimator.ofFloat(startHeight, 0);
            animator.setDuration(600);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float fraction = animation.getAnimatedFraction();
                    float alpha = 1f - fraction;
                    float height = (Float)animation.getAnimatedValue();
                    _loadingBar.setAlpha(alpha);
                    _loadingBar.getLayoutParams().height = (int)height;
                    _loadingBar.requestLayout();
                }
            });
            animator.start();
        } else if (_pendingInstanceCount == 0) {
            // There are some warnings
            _displayText.setText(R.string.could_not_fetch_all_profiles);
            _warningIcon.setVisibility(View.VISIBLE);
            _progressBar.setVisibility(View.GONE);
            _loadingBar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Display a dialog with all the warnings
                    StringBuilder warningsTextBuilder = new StringBuilder();
                    for (String warning : _warnings) {
                        warningsTextBuilder.append("- ").append(warning).append('\n');
                    }
                    ErrorDialog.show(getContext(), R.string.warnings_list, warningsTextBuilder.toString());
                }
            });
        }
    }

    /**
     * Downloads a VPN profile and connects to it if all went well.
     *
     * @param instance      The VPN provider.
     * @param discoveredAPI The discovered API.
     * @param profile       The profile to download.
     */
    private void _downloadProfileAndConnect(@NonNull final Instance instance, @NonNull DiscoveredAPI discoveredAPI, @NonNull final Profile profile) {
        final ProgressDialog dialog = ProgressDialog.show(getContext(),
                getString(R.string.progress_dialog_title),
                getString(R.string.vpn_profile_download_message),
                true,
                false);
        String uniqueName = "Android_" + System.currentTimeMillis() / 1000L;
        String requestData = "config_name=" + uniqueName + "&profile_id=" + profile.getProfileId();
        String url = discoveredAPI.getCreateConfigAPI();
        _apiService.postResource(url, requestData, true, new APIService.Callback<byte[]>() {

            @Override
            public void onSuccess(byte[] result) {
                String vpnConfig = new String(result);
                String configName = FormattingUtils.formatVPNProfileName(getContext(), instance, profile);
                VpnProfile vpnProfile = _vpnService.importConfig(vpnConfig, configName);
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
                    ErrorDialog.show(getContext(), R.string.error_dialog_title, R.string.error_importing_profile);
                }
            }

            @Override
            public void onError(String errorMessage) {
                dialog.dismiss();
                ErrorDialog.show(getContext(), R.string.error_dialog_title, getString(R.string.error_fetching_profile, errorMessage));
                Log.e(TAG, "Error fetching profile: " + errorMessage);
                if (errorMessage.equals(APIService.USER_NOT_AUTHORIZED_ERROR)) {
                    // Token is not valid anymore.
                    _historyService.removeAccessTokens(instance.getSanitizedBaseURI());
                    _historyService.removeDiscoveredAPI(instance.getSanitizedBaseURI());
                    Log.e(TAG, "API returned unauthorized error, removed cache for provider.");
                }
            }
        });
    }

    /**
     * Called when the user clicks on the 'Add Provider' button.
     */
    @OnClick(R.id.addProvider)
    protected void onAddProviderClicked() {
        ((MainActivity)getActivity()).openFragment(new ProviderSelectionFragment(), true);
    }
}
