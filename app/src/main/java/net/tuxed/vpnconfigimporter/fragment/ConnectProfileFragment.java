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
import net.tuxed.vpnconfigimporter.adapter.ProfileAdapter;
import net.tuxed.vpnconfigimporter.adapter.ProviderAdapter;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.service.ConfigurationService;
import net.tuxed.vpnconfigimporter.service.PreferencesService;
import net.tuxed.vpnconfigimporter.utils.ItemClickSupport;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * The fragment showing the provider list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class ConnectProfileFragment extends Fragment {

    @BindView(R.id.profilesList)
    protected RecyclerView _profileList;

    @Inject
    protected PreferencesService _preferencesService;

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
                // TODO
                Toast.makeText(recyclerView.getContext(), "Download profile now", Toast.LENGTH_SHORT).show();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        _fetchAvailableProfiles();
    }

    private void _fetchAvailableProfiles() {
        String url = _preferencesService.getConnectionBaseUrl() + "/portal/api/pool_list";
        // TODO fetch!

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _unbinder.unbind();
    }
}
