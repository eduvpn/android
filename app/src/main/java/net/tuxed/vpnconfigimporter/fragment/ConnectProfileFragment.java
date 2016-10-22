package net.tuxed.vpnconfigimporter.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.MainActivity;
import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.adapter.ProfileAdapter;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.entity.SavedProfile;
import net.tuxed.vpnconfigimporter.service.APIService;
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
import butterknife.Unbinder;
import de.blinkt.openvpn.VpnProfile;

/**
 * The fragment showing the provider list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class ConnectProfileFragment extends Fragment {

    private static final String TAG = ConnectProfileFragment.class.getName();

    @BindView(R.id.profilesList)
    protected RecyclerView _profileList;

    @BindView(R.id.hintText)
    protected TextView _hintText;

    @Inject
    protected PreferencesService _preferencesService;

    @Inject
    protected VPNService _vpnService;

    @Inject
    protected APIService _apiService;

    @Inject
    protected HistoryService _historyService;

    @Inject
    protected SerializerService _serializerService;

    private Unbinder _unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connect_profile, container, false);
        _unbinder = ButterKnife.bind(this, view);
        EduVPNApplication.get(view.getContext()).component().inject(this);
        _profileList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        _profileList.setAdapter(new ProfileAdapter(_preferencesService.getCurrentInstance()));
        ItemClickSupport.addTo(_profileList).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                Profile profile = ((ProfileAdapter)recyclerView.getAdapter()).getItem(position);
                _selectProfile(profile);
            }
        });
        return view;
    }

    /**
     * Generates a new config name.
     */
    private String _generateConfigName() {
        return "Android_" + System.currentTimeMillis() / 1000L;
    }

    /**
     * Downloads, imports, and opens the selected VPN profile.
     *
     * @param profile The profile to download.
     */
    private void _selectProfile(final Profile profile) {
        // Maybe we already have a downloaded profile for these criteria
        final Instance instance = _preferencesService.getCurrentInstance();
        SavedProfile savedProfile = _historyService.getCachedSavedProfile(instance.getSanitizedBaseURI(), profile.getProfileId());
        if (savedProfile != null) {
            // No need to download, continue immediately to the home screen.
            Toast.makeText(getContext(), R.string.profile_already_downloaded_select, Toast.LENGTH_LONG).show();
            ((MainActivity)getActivity()).openFragment(new HomeFragment(), false);
            return;
        }
        // Display loading message to the user
        _hintText.setText(R.string.downloading_profile);
        _hintText.setVisibility(View.VISIBLE);
        _profileList.setVisibility(View.GONE);
        final String uniqueName = _generateConfigName();
        String requestData = "config_name=" + uniqueName + "&profile_id=" + profile.getProfileId();
        String url = _preferencesService.getCurrentDiscoveredAPI().getCreateConfigAPI();
        _apiService.postResource(url, requestData, true, new APIService.Callback<byte[]>() {
            @Override
            public void onSuccess(byte[] result) {
                String vpnConfig = new String(result);
                String configName = FormattingUtils.formatVPNProfileName(getContext(), instance, profile);
                VpnProfile vpnProfile = _vpnService.importConfig(vpnConfig, configName);
                if (vpnProfile != null) {
                    if (getActivity() != null) {
                        // Cache the profile
                        SavedProfile savedProfile = new SavedProfile(instance, profile, vpnProfile.getUUIDString());
                        _historyService.cacheSavedProfile(savedProfile);
                        // Go back to the home screen and display a toast
                        Toast.makeText(getContext(), R.string.select_downloaded_profile, Toast.LENGTH_LONG).show();
                        ((MainActivity)getActivity()).openFragment(new HomeFragment(), false);

                    }
                } else {
                    _displayError(getString(R.string.error_importing_profile));
                }
            }

            @Override
            public void onError(String errorMessage) {
                _displayError(errorMessage);
                Log.e(TAG, "Error fetching profile: " + errorMessage);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        _fetchAvailableProfiles();
    }

    /**
     * Fetches the available profiles from the API, and puts them inside the list.
     */
    private void _fetchAvailableProfiles() {
        String url = _preferencesService.getCurrentDiscoveredAPI().getProfileListAPI();
        _apiService.getJSON(url, true, new APIService.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    List<Profile> profileList = _serializerService.deserializeProfileList(result);
                    ((ProfileAdapter)_profileList.getAdapter()).setItems(profileList);
                    _hintText.setVisibility(View.GONE);
                } catch (SerializerService.UnknownFormatException ex) {
                    Log.e(TAG, "Error while serializing profile list!", ex);
                    _displayError(ex.toString());
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error while fetching profile list: " + errorMessage);
                _displayError(errorMessage);
            }
        });

    }

    private void _displayError(String errorMessage) {
        _hintText.setText(R.string.error_loading_profiles);
        _hintText.setVisibility(View.VISIBLE);
        ErrorDialog.show(getContext(), R.string.error_dialog_title, errorMessage);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _unbinder.unbind();
    }
}
