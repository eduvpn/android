package net.tuxed.vpnconfigimporter.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import net.tuxed.vpnconfigimporter.EduVPNApplication;
import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.service.APIService;
import net.tuxed.vpnconfigimporter.service.ConnectionService;
import net.tuxed.vpnconfigimporter.service.SerializerService;
import net.tuxed.vpnconfigimporter.utils.ErrorDialog;
import net.tuxed.vpnconfigimporter.utils.Log;

import org.json.JSONObject;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static net.tuxed.vpnconfigimporter.Constants.API_DISCOVERY_POSTFIX;

/**
 * Fragment where you can give the URL to a custom provider.
 * Created by Daniel Zolnai on 2016-10-11.
 */
public class CustomProviderFragment extends Fragment {

    private static final String TAG = CustomProviderFragment.class.getName();

    private Unbinder _unbinder;

    @BindView(R.id.custom_provider_url)
    protected EditText _customProviderUrl;

    @Inject
    protected ConnectionService _connectionService;

    @Inject
    protected APIService _apiService;

    @Inject
    protected SerializerService _serializerService;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_custom_provider, container, false);
        _unbinder = ButterKnife.bind(this, view);
        EduVPNApplication.get(view.getContext()).component().inject(this);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Put the cursor in the field and show the keyboard automatically.
        _customProviderUrl.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(_customProviderUrl, InputMethodManager.SHOW_IMPLICIT);
    }

    @OnClick(R.id.custom_provider_connect)
    protected void onConnectClicked() {
        String prefix = getContext().getString(R.string.custom_provider_prefix);
        String postfix = _customProviderUrl.getText().toString();
        String url = prefix + postfix;
        if (getActivity() != null) {
            final Instance customProviderInstance = _createCustomProviderInstance(getActivity(), url);
            final ProgressDialog dialog = ProgressDialog.show(getContext(), getString(R.string.api_discovery_title), getString(R.string.api_discovery_message), true);
            // Discover the API
            _apiService.getJSON(customProviderInstance.getSanitizedBaseUri() + API_DISCOVERY_POSTFIX, false, new APIService.Callback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        DiscoveredAPI discoveredAPI = _serializerService.deserializeDiscoveredAPI(result);
                        dialog.dismiss();
                        _connectionService.initiateConnection(getActivity(), customProviderInstance, discoveredAPI, null);
                    } catch (SerializerService.UnknownFormatException ex) {
                        ErrorDialog.show(getContext(), R.string.error_dialog_title, getString(R.string.custom_api_discovery_error, ex.toString()));
                        Log.e(TAG, "Error while parsing discovered API", ex);
                        dialog.dismiss();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    dialog.dismiss();
                    Log.e(TAG, "Error fetching discovered API: " + errorMessage);
                    ErrorDialog.show(getContext(), R.string.error_dialog_title, getString(R.string.custom_api_discovery_error, errorMessage));
                }
            });
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
