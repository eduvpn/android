package net.tuxed.vpnconfigimporter.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.MainActivity;
import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.adapter.SavedProfileAdapter;
import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.entity.SavedProfile;
import net.tuxed.vpnconfigimporter.service.HistoryService;
import net.tuxed.vpnconfigimporter.service.PreferencesService;
import net.tuxed.vpnconfigimporter.service.VPNService;
import net.tuxed.vpnconfigimporter.utils.FormattingUtils;
import net.tuxed.vpnconfigimporter.utils.ItemClickSupport;

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

    @Inject
    protected HistoryService _historyService;

    @Inject
    protected VPNService _vpnService;

    @Inject
    protected PreferencesService _preferencesService;


    @BindView(R.id.savedProfileList)
    protected RecyclerView _savedProfileList;

    private Unbinder _unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View view= inflater.inflate(R.layout.fragment_home, container, false);
        _unbinder = ButterKnife.bind(this, view);
        EduVPNApplication.get(view.getContext()).component().inject(this);
        _savedProfileList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        List<SavedProfile> savedProfileList = _historyService.getSavedProfileList();
        if (savedProfileList == null) {
            // TODO show text to add profile.
        } else {
            _savedProfileList.setAdapter(new SavedProfileAdapter(savedProfileList));
        }
        ItemClickSupport.addTo(_savedProfileList).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                SavedProfileAdapter adapter = (SavedProfileAdapter)recyclerView.getAdapter();
                SavedProfile savedProfile = adapter.getItem(position);
                String profileUUID = savedProfile.getProfileUUID();
                ProfileManager profileManager = ProfileManager.getInstance(getContext());
                VpnProfile selectedProfile = null;
                for (VpnProfile vpnProfile : profileManager.getProfiles()) {
                    if (vpnProfile.getUUIDString().equals(profileUUID)) {
                        selectedProfile = vpnProfile;
                        break;
                    }
                }
                if (selectedProfile != null) {
                    _preferencesService.currentInstance(savedProfile.getInstance());
                    _preferencesService.currentProfile(savedProfile.getProfile());
                    // In the optimal case, we have an access token and a discovered API
                    String accessToken = _historyService.getCachedAccessToken(savedProfile.getInstance().getSanitizedBaseUri());
                    if (accessToken != null) {
                        _preferencesService.currentAccessToken(accessToken);
                    }
                    DiscoveredAPI discoveredAPI = _historyService.getCachedDiscoveredAPI(savedProfile.getInstance().getSanitizedBaseUri());
                    if (discoveredAPI != null) {
                        _preferencesService.currentDiscoveredAPI(discoveredAPI);
                    }
                    // TODO handle case of any of the two missing
                    _vpnService.connect(getActivity(), selectedProfile);
                    ((MainActivity)getActivity()).openFragment(new ConnectionStatusFragment(), false);
                } else {
                    // TODO, show error
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
