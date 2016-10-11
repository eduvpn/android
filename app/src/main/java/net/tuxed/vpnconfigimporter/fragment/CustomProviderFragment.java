package net.tuxed.vpnconfigimporter.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.MainActivity;
import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.service.PreferencesService;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Fragment where you can give the URL to a custom provider.
 * Created by Daniel Zolnai on 2016-10-11.
 */
public class CustomProviderFragment extends Fragment {

    private Unbinder _unbinder;

    @BindView(R.id.custom_provider_url)
    protected EditText _customProviderUrl;

    @Inject
    protected PreferencesService _preferencesService;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_custom_provider, container, false);
        _unbinder = ButterKnife.bind(this, view);
        EduVPNApplication.get(view.getContext()).component().inject(this);
        return view;
    }

    @OnClick(R.id.custom_provider_connect)
    protected void onConnectClicked() {
        String prefix = getContext().getString(R.string.custom_provider_prefix);
        String postfix = _customProviderUrl.getText().toString();
        String url = prefix + postfix;
        if (getActivity() != null) {
            MainActivity mainActivity = (MainActivity)getActivity();
            _preferencesService.saveConnectionInstance(_createCustomProviderInstance(mainActivity, url));
            mainActivity.initiateConnection(url);
        }
    }

    private Instance _createCustomProviderInstance(Context context, String baseUri) {
        return new Instance(baseUri, context.getString(R.string.custom_provider_display_name), null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _unbinder.unbind();
    }
}
