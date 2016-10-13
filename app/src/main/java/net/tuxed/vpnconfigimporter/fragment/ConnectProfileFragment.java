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
import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.adapter.ProfileAdapter;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.service.APIService;
import net.tuxed.vpnconfigimporter.service.PreferencesService;
import net.tuxed.vpnconfigimporter.service.SerializerService;
import net.tuxed.vpnconfigimporter.utils.ItemClickSupport;
import net.tuxed.vpnconfigimporter.utils.Log;

import org.json.JSONObject;

import java.util.List;

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

    @BindView(R.id.hintText)
    protected TextView _hintText;

    @Inject
    protected PreferencesService _preferencesService;

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

    /**
     * Fetches the available profiles from the API, and puts them inside the list.
     */
    private void _fetchAvailableProfiles() {
        String url = _preferencesService.getConnectionBaseUrl() + "/portal/api/pool_list";
        _apiService.getJSON(url, new APIService.Callback() {
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
