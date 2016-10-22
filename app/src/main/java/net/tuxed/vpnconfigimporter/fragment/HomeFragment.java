package net.tuxed.vpnconfigimporter.fragment;

import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.tuxed.vpnconfigimporter.Constants;
import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.MainActivity;
import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.adapter.ProfileAdapter;
import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.service.APIService;
import net.tuxed.vpnconfigimporter.service.ConfigurationService;
import net.tuxed.vpnconfigimporter.service.ConnectionService;
import net.tuxed.vpnconfigimporter.service.HistoryService;
import net.tuxed.vpnconfigimporter.service.PreferencesService;
import net.tuxed.vpnconfigimporter.service.SerializerService;
import net.tuxed.vpnconfigimporter.service.VPNService;
import net.tuxed.vpnconfigimporter.utils.ErrorDialog;
import net.tuxed.vpnconfigimporter.utils.FormattingUtils;
import net.tuxed.vpnconfigimporter.utils.ItemClickSupport;
import net.tuxed.vpnconfigimporter.utils.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import de.blinkt.openvpn.VpnProfile;

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
        _profileList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        List<Instance> availableInstances = _configurationService.getInstanceList().getInstanceList();
        List<Pair<Instance, String>> instanceAccessTokenPairs = _pairTokensWithInstances(availableInstances);
        if (instanceAccessTokenPairs.size() == 0) {
            _noProvidersYet.setVisibility(View.VISIBLE);
            _profileList.setVisibility(View.GONE);
        } else {
            _noProvidersYet.setVisibility(View.GONE);
            _profileList.setVisibility(View.VISIBLE);
            ProfileAdapter adapter = new ProfileAdapter(null);
            _profileList.setAdapter(adapter);
            _fillList(adapter, instanceAccessTokenPairs);
        }

        ItemClickSupport.addTo(_profileList).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                /**
                 SavedProfileAdapter adapter = (SavedProfileAdapter)recyclerView.getAdapter();
                 SavedProfile savedProfile = adapter.getItem(position);
                 String profileUUID = savedProfile.getProfileUUID();
                 VpnProfile selectedProfile= _vpnService.getProfileWithUUID(profileUUID);
                 if (selectedProfile != null) {
                 _preferencesService.currentInstance(savedProfile.getInstance());
                 _preferencesService.currentProfile(savedProfile.getProfile());
                 // In the optimal case, we have an access token and a discovered API
                 String accessToken = _historyService.getCachedAccessToken(savedProfile.getInstance().getSanitizedBaseURI());
                 DiscoveredAPI discoveredAPI = _historyService.getCachedDiscoveredAPI(savedProfile.getInstance().getSanitizedBaseURI());
                 if (accessToken != null && discoveredAPI != null) {
                 _preferencesService.currentAccessToken(accessToken);
                 _preferencesService.currentDiscoveredAPI(discoveredAPI);
                 _vpnService.connect(getActivity(), selectedProfile);
                 ((MainActivity)getActivity()).openFragment(new ConnectionStatusFragment(), false);
                 return;
                 }
                 // Better case: we have a token, but no discovered API.
                 // We just have to discover it, and then connect.
                 if (accessToken != null) {
                 _preferencesService.currentAccessToken(accessToken);
                 _discoverAPIAndThenConnectOrLogin(savedProfile.getInstance(), selectedProfile, null);
                 return;
                 }
                 // Worse case: no token
                 _discoverAPIAndThenConnectOrLogin(savedProfile.getInstance(), null, savedProfile.getProfileUUID());
                 } else {
                 // This should not happen though.
                 ErrorDialog.show(getContext(), R.string.error_dialog_title, R.string.selected_profile_does_not_exist);
                 // Remove it
                 _historyService.removeSavedProfile(savedProfile);
                 }**/
            }
        });
        ItemClickSupport.addTo(_profileList).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
                ProfileAdapter adapter = (ProfileAdapter)recyclerView.getAdapter();
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

    private List<Pair<Instance, String>> _pairTokensWithInstances(List<Instance> availableInstances) {
        List<Pair<Instance, String>> result = new ArrayList<>();
        for (Instance instance : availableInstances) {
            String accessToken = _historyService.getCachedAccessToken(instance.getSanitizedBaseURI());
            if (accessToken != null) {
                result.add(new Pair<>(instance, accessToken));
            }
        }
        return result;
    }


    private void _fillList(final ProfileAdapter adapter, List<Pair<Instance, String>> instanceAccessTokenPairs) {
        _pendingInstanceCount = instanceAccessTokenPairs.size();
        _warnings = new ArrayList<>();
        for (Pair<Instance, String> instanceAccessTokenPair : instanceAccessTokenPairs) {
            final Instance instance = instanceAccessTokenPair.first;
            final String accessToken = instanceAccessTokenPair.second;
            DiscoveredAPI discoveredAPI = _historyService.getCachedDiscoveredAPI(instance.getSanitizedBaseURI());
            if (discoveredAPI != null) {
                // We got everything, fetch the available profiles.
                _fetchProfileList(adapter, instance, discoveredAPI, accessToken);
            } else {
                _apiService.getJSON(instance.getSanitizedBaseURI() + Constants.API_DISCOVERY_POSTFIX, false, new APIService.Callback<JSONObject>() {
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

    private void _fetchProfileList(final ProfileAdapter adapter, final Instance instance, DiscoveredAPI discoveredAPI, String accessToken) {
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
                _checkLoadingFinished();
            }
        });
    }

    private void _checkLoadingFinished() {
        _pendingInstanceCount--;
        if (_pendingInstanceCount == 0 && _warnings.size() == 0) {
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
     * Discovers the API, and then immediately connects using the VPN profile. Discovering the API is required because we
     * need the URLs to the messages API.
     *
     * @param instance        The provider to discover.
     * @param selectedProfile The profile to connect to on success.
     */
    private void _discoverAPIAndThenConnectOrLogin(@NonNull final Instance instance, @Nullable final VpnProfile selectedProfile, @Nullable final String profileUUID) {
        final ProgressDialog dialog = ProgressDialog.show(getContext(), getString(R.string.api_discovery_title), getString(R.string.api_discovery_message), true);
    }

    @OnClick(R.id.addProvider)
    public void onAddProviderClicked() {
        ((MainActivity)getActivity()).openFragment(new ProviderSelectionFragment(), true);
    }
}
