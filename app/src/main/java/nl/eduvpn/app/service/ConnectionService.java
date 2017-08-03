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

package nl.eduvpn.app.service;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.widget.Toast;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.CodeVerifierUtil;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenResponse;
import net.openid.appauth.browser.BrowserBlacklist;
import net.openid.appauth.browser.VersionedBrowserMatcher;

import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import nl.eduvpn.app.MainActivity;
import nl.eduvpn.app.R;
import nl.eduvpn.app.entity.DiscoveredAPI;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.utils.ErrorDialog;
import nl.eduvpn.app.utils.Log;

/**
 * The connection service takes care of building up the URLs and validating the result.
 * Created by Daniel Zolnai on 2016-10-11.
 */
public class ConnectionService {

    private static final String TAG = ConnectionService.class.getName();

    private static final String SCOPE = "config";
    private static final String RESPONSE_TYPE = ResponseTypeValues.CODE;
    private static final String REDIRECT_URI = "org.eduvpn.app:/api/callback";
    private static final String CLIENT_ID = "org.eduvpn.app";

    private static final int REQUEST_CODE_APP_AUTH = 100; // This is not used, since we only get one type of request for the redirect URL.


    private final PreferencesService _preferencesService;
    private final HistoryService _historyService;
    private final SecurityService _securityService;
    private AuthorizationService _authorizationService;
    private String _accessToken;

    /**
     * Constructor.
     *
     * @param preferencesService The preferences service used to store temporary data.
     * @param historyService     History service for storing data for long-term usage
     * @param securityService    For security related tasks.
     */
    public ConnectionService(PreferencesService preferencesService, HistoryService historyService,
                             SecurityService securityService) {
        _preferencesService = preferencesService;
        _securityService = securityService;
        _historyService = historyService;
        _accessToken = preferencesService.getCurrentAccessToken();
    }

    public void warmUp(Activity activity) {
        if (!_preferencesService.getAppSettings().useCustomTabs()) {
            // We do not allow any custom tab implementation.
            _authorizationService = new AuthorizationService(activity, new AppAuthConfiguration.Builder()
                    .setBrowserMatcher(new BrowserBlacklist(
                            VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                            VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB)
                    )
                    .build());
        } else {
            // Default behavior
            _authorizationService = new AuthorizationService(activity);
        }
    }


    /**
     * Initiates a connection to the VPN provider instance.
     *
     * @param activity      The current activity.
     * @param instance      The instance to connect to.
     * @param discoveredAPI The discovered API which has the URL.
     */
    public void initiateConnection(@NonNull final Activity activity, @NonNull final Instance instance, @NonNull final DiscoveredAPI discoveredAPI) {
        Observable.defer(new Callable<ObservableSource<AuthorizationRequest>>() {
            @Override
            public ObservableSource<AuthorizationRequest> call() throws Exception {
                String stateString = _securityService.generateSecureRandomString(32);
                _preferencesService.currentInstance(instance);
                _preferencesService.storeCurrentDiscoveredAPI(discoveredAPI);
                AuthorizationServiceConfiguration serviceConfig =
                        new AuthorizationServiceConfiguration(
                                Uri.parse(discoveredAPI.getAuthorizationEndpoint()),
                                Uri.parse(discoveredAPI.getTokenEndpoint()),
                                null);
                AuthorizationRequest.Builder authRequestBuilder =
                        new AuthorizationRequest.Builder(
                                serviceConfig, // the authorization service configuration
                                CLIENT_ID, // the client ID, typically pre-registered and static
                                ResponseTypeValues.CODE, // the response_type value: we want a code
                                Uri.parse(REDIRECT_URI)) // the redirect URI to which the auth response is sent
                                .setScope(SCOPE)
                                .setCodeVerifier(CodeVerifierUtil.generateRandomCodeVerifier()) // Use S256 challenge method if possible
                                .setResponseType(RESPONSE_TYPE)
                                .setState(stateString);
                return Observable.just(authRequestBuilder.build());
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<AuthorizationRequest>() {
                    @Override
                    public void accept(AuthorizationRequest authorizationRequest) throws Exception {
                        if (_authorizationService == null) {
                            warmUp(activity);
                            Log.w(TAG, "WARNING: You did not call warmUp() on the service before making your first call! You might see increased waiting times before opening the browser.");
                        }
                        _authorizationService.performAuthorizationRequest(
                                authorizationRequest,
                                PendingIntent.getActivity(activity, REQUEST_CODE_APP_AUTH, new Intent(activity, MainActivity.class), 0));

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        ErrorDialog.show(activity, R.string.error_dialog_title, throwable.getMessage());
                    }
                });
    }

    /**
     * Parses the authorization response and retrieves the access token.
     *
     * @param authorizationResponse The auth response to process.
     * @param activity              The current activity.
     */
    public void parseAuthorizationResponse(@NonNull AuthorizationResponse authorizationResponse, final Activity activity) {
        Log.i(TAG, "Got auth response: " + authorizationResponse.jsonSerializeString());
        _authorizationService.performTokenRequest(
                authorizationResponse.createTokenExchangeRequest(),
                new AuthorizationService.TokenResponseCallback() {
                    @Override
                    public void onTokenRequestCompleted(TokenResponse tokenResponse, AuthorizationException ex) {
                        if (tokenResponse != null) {
                            // exchange succeeded
                            _processTokenExchangeResponse(tokenResponse, activity);
                        } else {
                            // authorization failed, check ex for more details
                            ErrorDialog.show(activity,
                                    R.string.authorization_error_title,
                                    activity.getString(R.string.authorization_error_message, ex.error, ex.code, ex.getMessage()));
                        }
                        _authorizationService.dispose();
                        _authorizationService = null;
                    }
                });

    }

    /**
     * Process the the exchanged token.
     *
     * @param tokenResponse The response from the token exchange.
     * @param activity      The current activity.
     */
    private void _processTokenExchangeResponse(TokenResponse tokenResponse, Activity activity) {
        String accessToken = tokenResponse.accessToken;
        if (accessToken == null || accessToken.isEmpty()) {
            ErrorDialog.show(activity, R.string.error_dialog_title, R.string.error_access_token_missing);
            return;
        }
        // Save the access token
        _accessToken = accessToken;
        _preferencesService.storeCurrentAccessToken(accessToken);
        // Save the access token for later use.
        _historyService.cacheAccessToken(_preferencesService.getCurrentInstance(), _accessToken);
        Toast.makeText(activity, R.string.provider_added_new_configs_available, Toast.LENGTH_LONG).show();
    }

    /**
     * Returns the access token which authenticates against the current API.
     *
     * @return The access token used to authenticate.
     */
    public String getAccessToken() {
        return _accessToken;
    }

    /**
     * Sets and saved the current access token to use with the requests.
     *
     * @param accessToken The access token to use for getting resources.
     */
    public void setAccessToken(String accessToken) {
        _accessToken = accessToken;
        _preferencesService.storeCurrentAccessToken(accessToken);
    }
}
