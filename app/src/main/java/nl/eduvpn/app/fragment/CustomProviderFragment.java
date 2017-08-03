/*
 *  This file is part of eduVPN.
 *
 *     eduVPN is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     eduVPN is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.eduvpn.app.fragment;

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

import nl.eduvpn.app.EduVPNApplication;
import nl.eduvpn.app.R;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.service.APIService;
import nl.eduvpn.app.service.ConnectionService;
import nl.eduvpn.app.service.SerializerService;
import nl.eduvpn.app.utils.ErrorDialog;
import nl.eduvpn.app.utils.Log;

import org.json.JSONObject;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static nl.eduvpn.app.Constants.API_DISCOVERY_POSTFIX;

/**
 * Fragment where you can give the URL to a custom provider.
 * Created by Daniel Zolnai on 2016-10-11.
 */
public class CustomProviderFragment extends Fragment {

    private static final String TAG = CustomProviderFragment.class.getName();
    public static final String EXTRA_AUTHORIZATION_TYPE = "extra_authorization_type";

    private Unbinder _unbinder;

    @BindView(R.id.custom_provider_url)
    protected EditText _customProviderUrl;

    @Inject
    protected ConnectionService _connectionService;

    @Inject
    protected APIService _apiService;

    @Inject
    protected SerializerService _serializerService;

    private @AuthorizationType Integer _authorizationType;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_AUTHORIZATION_TYPE)) {
            //noinspection WrongConstant
            _authorizationType = savedInstanceState.getInt(EXTRA_AUTHORIZATION_TYPE);
        } else if (getArguments() != null && getArguments().containsKey(EXTRA_AUTHORIZATION_TYPE)) {
            //noinspection WrongConstant
            _authorizationType = getArguments().getInt(EXTRA_AUTHORIZATION_TYPE);
        }
        if (_authorizationType == null) {
            throw new RuntimeException("Connection type not provided!");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_AUTHORIZATION_TYPE, _authorizationType);
    }

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
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(_customProviderUrl, InputMethodManager.SHOW_IMPLICIT);
    }

    @OnClick(R.id.custom_provider_connect)
    protected void onConnectClicked() {
        String prefix = getContext().getString(R.string.custom_provider_prefix);
        String postfix = _customProviderUrl.getText().toString();
        String url = prefix + postfix;
        if (getActivity() != null) {
            final Instance customProviderInstance = _createCustomProviderInstance(url, _authorizationType);
            final ProgressDialog dialog = ProgressDialog.show(getContext(), getString(R.string.progress_dialog_title), getString(R.string.api_discovery_message), true);
            // Discover the API
            _apiService.getJSON(customProviderInstance.getSanitizedBaseURI() + API_DISCOVERY_POSTFIX, false, new APIService.Callback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        DiscoveredAPI discoveredAPI = _serializerService.deserializeDiscoveredAPI(result);
                        dialog.dismiss();
                        _connectionService.initiateConnection(getActivity(), customProviderInstance, discoveredAPI);
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

    /**
     * Creates a custom provider instance for caching.
     *
     * @param baseUri The base URI of the instance.
     * @return A new instance.
     */
    private Instance _createCustomProviderInstance(String baseUri, @AuthorizationType int authorizationType) {
        return new Instance(baseUri, getString(R.string.custom_provider_display_name), null, authorizationType, true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _unbinder.unbind();
    }
}
