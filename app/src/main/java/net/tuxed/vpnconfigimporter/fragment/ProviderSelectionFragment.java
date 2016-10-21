package net.tuxed.vpnconfigimporter.fragment;

import android.app.ProgressDialog;
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
import net.tuxed.vpnconfigimporter.adapter.ProviderAdapter;
import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.SavedToken;
import net.tuxed.vpnconfigimporter.service.APIService;
import net.tuxed.vpnconfigimporter.service.ConfigurationService;
import net.tuxed.vpnconfigimporter.service.ConnectionService;
import net.tuxed.vpnconfigimporter.service.HistoryService;
import net.tuxed.vpnconfigimporter.service.SerializerService;
import net.tuxed.vpnconfigimporter.utils.ItemClickSupport;
import net.tuxed.vpnconfigimporter.utils.Log;

import org.json.JSONObject;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * The fragment showing the provider list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class ProviderSelectionFragment extends Fragment {

    private static final String TAG = ProviderSelectionFragment.class.getName();

    @BindView(R.id.providersList)
    protected RecyclerView _providersList;

    @Inject
    protected ConfigurationService _configurationService;

    @Inject
    protected APIService _apiService;

    @Inject
    protected SerializerService _serializerService;

    @Inject
    protected ConnectionService _connectionService;

    @Inject
    protected HistoryService _historyService;

    private Unbinder _unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_provider_selection, container, false);
        _unbinder = ButterKnife.bind(this, view);
        EduVPNApplication.get(view.getContext()).component().inject(this);
        _providersList.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        _providersList.setAdapter(new ProviderAdapter(_configurationService));
        ItemClickSupport.addTo(_providersList).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                Instance instance = ((ProviderAdapter)recyclerView.getAdapter()).getItem(position);
                if (instance == null) {
                    if (getActivity() != null) {
                        MainActivity activity = (MainActivity)getActivity();
                        activity.openFragment(new CustomProviderFragment(), true);
                    }
                } else {
                    _connectToApi(instance);
                }
            }
        });
        // When clicked long on an item, display its name in a toast.
        ItemClickSupport.addTo(_providersList).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
                Instance instance = ((ProviderAdapter)recyclerView.getAdapter()).getItem(position);
                String name = instance == null ? getString(R.string.display_other_name) : instance.getDisplayName();
                Toast.makeText(recyclerView.getContext(), name, Toast.LENGTH_LONG).show();
                return true;
            }
        });
        return view;
    }

    /**
     * Starts connecting to an API provider.
     * @param instance The instance to connect to.
     */
    private void _connectToApi(final Instance instance) {
        // If there's a saved access token, continue immediately to the config selector.
        String savedToken = _historyService.getCachedAccessToken(instance.getSanitizedBaseUri());
        if (savedToken != null) {
            _connectionService.setAccessToken(savedToken);
            // Open the config selector immediately. If it would throw a 401, then we will trigger a login afterwards.
            ((MainActivity)getActivity()).openFragment(new ConnectProfileFragment(), true);
            return;
        }
        // Check if there's a cached discovered API
        DiscoveredAPI discoveredAPI = _historyService.getCachedDiscoveredAPI(instance.getSanitizedBaseUri());
        if (discoveredAPI != null) {
            Log.d(TAG, "Cached discovered API found.");
            _connectionService.initiateConnection(getActivity(), instance, discoveredAPI);
            return;
        }
        Log.d(TAG, "No cached discovered API found, continuing with discovery.");
        final ProgressDialog dialog = ProgressDialog.show(getContext(), getString(R.string.api_discovery_title), getString(R.string.api_discovery_message), true);
        // Discover the API
        _apiService.getJSON(instance.getSanitizedBaseUri() + "/info.json", false, new APIService.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    DiscoveredAPI discoveredAPI = _serializerService.deserializeDiscoveredAPI(result);
                    dialog.dismiss();
                    // Cache the result
                    _historyService.cacheDiscoveredAPI(instance.getSanitizedBaseUri(), discoveredAPI);
                    _connectionService.initiateConnection(getActivity(), instance, discoveredAPI);
                } catch (SerializerService.UnknownFormatException ex) {
                    Log.e("ERROR", ex.getMessage());
                    // TODO show error.
                    dialog.dismiss();
                }
            }

            @Override
            public void onError(String errorMessage) {
                dialog.dismiss();
                // TODO show error message
                Log.e("ERROR", errorMessage);
            }
        });
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _connectionService.warmup();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _unbinder.unbind();
    }
}
