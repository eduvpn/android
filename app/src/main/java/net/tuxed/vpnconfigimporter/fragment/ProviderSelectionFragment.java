package net.tuxed.vpnconfigimporter.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.MainActivity;
import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.adapter.ProviderAdapter;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.service.ConfigurationService;
import net.tuxed.vpnconfigimporter.service.ConnectionService;
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
public class ProviderSelectionFragment extends Fragment {

    @BindView(R.id.providersList)
    protected RecyclerView _providersList;

    @Inject
    protected ConfigurationService _configurationService;

    @Inject
    protected ConnectionService _connectionService;

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
                        activity.openFragment(new CustomProviderFragment());
                    }
                } else {
                    if (getActivity() != null) {
                        _connectionService.initiateConnection(getActivity(), instance);
                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _unbinder.unbind();
    }
}
