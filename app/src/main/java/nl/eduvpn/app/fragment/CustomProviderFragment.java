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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.json.JSONObject;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import nl.eduvpn.app.EduVPNApplication;
import nl.eduvpn.app.R;
import nl.eduvpn.app.base.BaseFragment;
import nl.eduvpn.app.databinding.FragmentCustomProviderBinding;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.service.APIService;
import nl.eduvpn.app.service.ConnectionService;
import nl.eduvpn.app.service.SerializerService;
import nl.eduvpn.app.utils.ErrorDialog;
import nl.eduvpn.app.utils.Log;

import static nl.eduvpn.app.Constants.API_DISCOVERY_POSTFIX;

/**
 * Fragment where you can give the URL to a custom provider.
 * Created by Daniel Zolnai on 2016-10-11.
 */
public class CustomProviderFragment extends BaseFragment<FragmentCustomProviderBinding> {

    private static final String TAG = CustomProviderFragment.class.getName();

    @Inject
    protected ConnectionService _connectionService;

    @Inject
    protected APIService _apiService;

    @Inject
    protected SerializerService _serializerService;


    @Override
    protected int getLayout() {
        return R.layout.fragment_custom_provider;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EduVPNApplication.get(view.getContext()).component().inject(this);

        binding.customProviderConnect.setOnClickListener(v -> onConnectClicked());

        // Put the cursor in the field and show the keyboard automatically.
        binding.customProviderUrl.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(binding.customProviderUrl, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    protected void onConnectClicked() {
        String prefix = getString(R.string.custom_provider_prefix);
        String postfix = binding.customProviderUrl.getText().toString();
        String url = prefix + postfix;
        if (getActivity() != null) {
            final Instance customProviderInstance = _createCustomProviderInstance(url, AuthorizationType.LOCAL);
            final ProgressDialog dialog = ProgressDialog.show(getContext(), getString(R.string.progress_dialog_title), getString(R.string.api_discovery_message), true);
            // Discover the API
            _apiService.getJSON(customProviderInstance.getSanitizedBaseURI() + API_DISCOVERY_POSTFIX, null, new APIService.Callback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        DiscoveredAPI discoveredAPI = _serializerService.deserializeDiscoveredAPI(result);
                        dialog.dismiss();
                        Activity activity = getActivity();
                        if (activity != null && !activity.isFinishing()) {
                            _connectionService.initiateConnection(activity, customProviderInstance, discoveredAPI);
                        }
                    } catch (SerializerService.UnknownFormatException ex) {
                        Context context = getContext();
                        if (context != null){
                            ErrorDialog.show(context, R.string.error_dialog_title, context.getString(R.string.custom_api_discovery_error, ex.toString()));
                        }
                        Log.e(TAG, "Error while parsing discovered API", ex);
                        dialog.dismiss();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    dialog.dismiss();
                    Log.e(TAG, "Error fetching discovered API: " + errorMessage);
                    Context context = getContext();
                    if (context != null) {
                        ErrorDialog.show(context, R.string.error_dialog_title, getString(R.string.custom_api_discovery_error, errorMessage));
                    }
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
}
