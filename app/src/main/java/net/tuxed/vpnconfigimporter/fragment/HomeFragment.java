package net.tuxed.vpnconfigimporter.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.tuxed.vpnconfigimporter.Constants;
import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.MainActivity;
import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.adapter.SavedProfileAdapter;
import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.SavedProfile;
import net.tuxed.vpnconfigimporter.service.APIService;
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

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;

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

    @BindView(R.id.savedProfileList)
    protected RecyclerView _savedProfileList;

    @BindView(R.id.noProfilesYet)
    protected TextView _noProfilesYet;

    private Unbinder _unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        _unbinder = ButterKnife.bind(this, view);
        EduVPNApplication.get(view.getContext()).component().inject(this);
        _savedProfileList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        List<SavedProfile> savedProfileList = _historyService.getSavedProfileList();
        if (savedProfileList == null || savedProfileList.size() == 0) {
            _noProfilesYet.setVisibility(View.VISIBLE);
            _savedProfileList.setVisibility(View.GONE);
        } else {
            _noProfilesYet.setVisibility(View.GONE);
            _savedProfileList.setVisibility(View.VISIBLE);
            _savedProfileList.setAdapter(new SavedProfileAdapter(savedProfileList));
        }
        ItemClickSupport.addTo(_savedProfileList).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                SavedProfileAdapter adapter = (SavedProfileAdapter)recyclerView.getAdapter();
                SavedProfile savedProfile = adapter.getItem(position);
                String profileUUID = savedProfile.getProfileUUID();
                VpnProfile selectedProfile= _vpnService.getProfileWithUUID(profileUUID);
                if (selectedProfile != null) {
                    _preferencesService.currentInstance(savedProfile.getInstance());
                    _preferencesService.currentProfile(savedProfile.getProfile());
                    // In the optimal case, we have an access token and a discovered API
                    String accessToken = _historyService.getCachedAccessToken(savedProfile.getInstance().getSanitizedBaseUri());
                    DiscoveredAPI discoveredAPI = _historyService.getCachedDiscoveredAPI(savedProfile.getInstance().getSanitizedBaseUri());
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
                }
            }
        });
        ItemClickSupport.addTo(_savedProfileList).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
                SavedProfileAdapter adapter = (SavedProfileAdapter)recyclerView.getAdapter();
                SavedProfile savedProfile = adapter.getItem(position);
                Toast.makeText(getContext(), FormattingUtils.formatSavedProfileName(getContext(), savedProfile), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        return view;
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
        _apiService.getJSON(instance.getSanitizedBaseUri() + Constants.API_DISCOVERY_POSTFIX, false, new APIService.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    DiscoveredAPI discoveredAPI = _serializerService.deserializeDiscoveredAPI(result);
                    dialog.dismiss();
                    // Cache the result
                    _historyService.cacheDiscoveredAPI(instance.getSanitizedBaseUri(), discoveredAPI);
                    if (selectedProfile != null) {
                        // We got the discovered API, the token and the saved profile. Off we go.
                        _preferencesService.currentDiscoveredAPI(discoveredAPI);
                        _vpnService.connect(getActivity(), selectedProfile);
                        ((MainActivity)getActivity()).openFragment(new ConnectionStatusFragment(), false);
                    } else {
                        // We discovered the API, but still don't have an access token. We need to login again,
                        // and then autoconnect.
                        _connectionService.initiateConnection(getActivity(), instance, discoveredAPI, profileUUID);
                    }
                } catch (SerializerService.UnknownFormatException ex) {
                    Log.e(TAG, "Error parsing discovered API!", ex);
                    ErrorDialog.show(getContext(), R.string.error_dialog_title, ex.toString());
                    dialog.dismiss();
                }
            }

            @Override
            public void onError(String errorMessage) {
                dialog.dismiss();
                Log.e(TAG, "Error while fetching discovered API: " + errorMessage);
                ErrorDialog.show(getContext(), R.string.error_dialog_title, errorMessage);
            }
        });
    }

    @OnClick(R.id.addProvider)
    public void onAddProviderClicked() {
        ((MainActivity)getActivity()).openFragment(new ProviderSelectionFragment(), true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _unbinder.unbind();
    }
}
