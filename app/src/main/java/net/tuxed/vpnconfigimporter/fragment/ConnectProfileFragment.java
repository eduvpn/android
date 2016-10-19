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

import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.MainActivity;
import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.adapter.ProfileAdapter;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.service.APIService;
import net.tuxed.vpnconfigimporter.service.PreferencesService;
import net.tuxed.vpnconfigimporter.service.SerializerService;
import net.tuxed.vpnconfigimporter.service.VPNService;
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
    protected SerializerService _serializerService;

    private Unbinder _unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connect_profile, container, false);
        _unbinder = ButterKnife.bind(this, view);
        EduVPNApplication.get(view.getContext()).component().inject(this);
        _profileList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        _profileList.setAdapter(new ProfileAdapter(_preferencesService.getSavedInstance()));
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
        // Display loading message to the user
        _hintText.setText(R.string.downloading_profile);
        _hintText.setVisibility(View.VISIBLE);
        _profileList.setVisibility(View.GONE);
        final String configName = _generateConfigName();
        String requestData = "config_name=" + configName + "&pool_id=" + profile.getPoolId();
        String url = _preferencesService.getSavedDiscoveredAPI().getCreateConfigAPI();
        _apiService.postResource(url, requestData, true, new APIService.Callback<byte[]>() {
            @Override
            public void onSuccess(byte[] result) {
                String vpnConfig = new String(result);
                VpnProfile vpnProfile = _vpnService.importConfig(vpnConfig, configName);
                if (vpnProfile != null) {
                    if (getActivity() != null) {
                        _preferencesService.saveProfile(profile);
                        _vpnService.connect(getActivity(), vpnProfile);
                        ((MainActivity)getActivity()).openFragment(new ConnectionStatusFragment(), false);
                    }
                } else {
                    _displayError(getString(R.string.error_importing_profile));
                }
            }

            @Override
            public void onError(String errorMessage) {
                _displayError(errorMessage);
                Log.e("ERROR", errorMessage);
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
        String url = _preferencesService.getSavedDiscoveredAPI().getProfileListAPI();
        _apiService.getJSON(url, true, new APIService.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    List<Profile> profileList = _serializerService.deserializeProfileList(result);
                    ((ProfileAdapter)_profileList.getAdapter()).setItems(profileList);
                    _hintText.setVisibility(View.GONE);
                } catch (SerializerService.UnknownFormatException ex) {
                    _displayError(ex.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                _displayError(errorMessage);
            }
        });

    }

    private void _displayError(String errorMessage) {
        _hintText.setText(R.string.error_loading_profiles);
        _hintText.setVisibility(View.VISIBLE);
        Log.e("ERROR", errorMessage);
        // TODO display error dialog with longer text.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _unbinder.unbind();
    }
}
