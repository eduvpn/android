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
package nl.eduvpn.app.service

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.openid.appauth.*
import net.openid.appauth.browser.BrowserDenyList
import net.openid.appauth.browser.VersionedBrowserMatcher
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.DiscoveredAPI
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.utils.ErrorDialog.show
import nl.eduvpn.app.utils.Log

/**
 * The connection service takes care of building up the URLs and validating the result.
 * Created by Daniel Zolnai on 2016-10-11.
 *
 * @param preferencesService The preferences service used to store temporary data.
 * @param historyService     History service for storing data for long-term usage
 * @param securityService    For security related tasks.
 */
class ConnectionService(private val preferencesService: PreferencesService,
                        private val historyService: HistoryService,
                        private val securityService: SecurityService) {

    private var authorizationService: AuthorizationService? = null

    fun onStart(activity: Activity?) {
        authorizationService = if (!preferencesService.appSettings.useCustomTabs()) {
            // We do not allow any custom tab implementation.
            AuthorizationService(activity!!, AppAuthConfiguration.Builder()
                    .setBrowserMatcher(BrowserDenyList(
                            VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                            VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB)
                    )
                    .build())
        } else {
            // Default behavior
            AuthorizationService(activity!!)
        }
    }

    fun onStop() {
        if (authorizationService != null) {
            authorizationService!!.dispose()
            authorizationService = null
        }
    }

    /**
     * Initiates a connection to the VPN provider instance.
     *
     * @param activity      The current activity.
     * @param instance      The instance to connect to.
     * @param discoveredAPI The discovered API which has the URL.
     */
    fun initiateConnection(activity: Activity, instance: Instance, discoveredAPI: DiscoveredAPI): Disposable {
        return Observable.defer {
            val stateString = securityService.generateSecureRandomString(32)
            preferencesService.currentInstance = instance
            preferencesService.currentDiscoveredAPI = discoveredAPI
            val serviceConfig = buildAuthConfiguration(discoveredAPI)
            val authRequestBuilder = AuthorizationRequest.Builder(
                    serviceConfig,  // the authorization service configuration
                    CLIENT_ID,  // the client ID, typically pre-registered and static
                    ResponseTypeValues.CODE,  // the response_type value: we want a code
                    Uri.parse(REDIRECT_URI)) // the redirect URI to which the auth response is sent
                    .setScope(SCOPE)
                    .setCodeVerifier(CodeVerifierUtil.generateRandomCodeVerifier()) // Use S256 challenge method if possible
                    .setResponseType(RESPONSE_TYPE)
                    .setState(stateString)
            Observable.just(authRequestBuilder.build())
        }.subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ authorizationRequest: AuthorizationRequest? ->
                    if (authorizationService == null) {
                        throw RuntimeException("Please call onStart() on this service from your activity!")
                    }
                    if (!activity.isFinishing) {
                        val originalIntent = authorizationService!!.getAuthorizationRequestIntent(
                                authorizationRequest!!,
                                PendingIntent.getActivity(activity, REQUEST_CODE_APP_AUTH, Intent(activity, MainActivity::class.java), 0))
                        if (instance.authenticationUrlTemplate != null && instance.authenticationUrlTemplate.isNotEmpty() && originalIntent.getParcelableExtra<Parcelable?>("authIntent") != null && preferencesService.currentOrganization != null) {
                            val authIntent = originalIntent.getParcelableExtra<Intent>("authIntent")
                            if (authIntent != null && authIntent.dataString != null) {
                                val replacedUrl = instance.authenticationUrlTemplate
                                        .replace("@RETURN_TO@", Uri.encode(authIntent.dataString))
                                        .replace("@ORG_ID@", Uri.encode(preferencesService.currentOrganization!!.orgId))
                                authIntent.data = Uri.parse(replacedUrl)
                                originalIntent.putExtra("authIntent", authIntent)
                            }
                        }
                        activity.startActivity(originalIntent)
                    }
                }) { throwable: Throwable ->
                    if (!activity.isFinishing) {
                        show(activity, R.string.error_dialog_title, (throwable.message)!!)
                    }
                }
    }

    /**
     * Parses the authorization response and retrieves the access token.
     *
     * @param authorizationResponse The auth response to process.
     * @param activity              The current activity.
     * @param callback              Callback which is called when the states are updated.
     */
    fun parseAuthorizationResponse(authorizationResponse: AuthorizationResponse, activity: Activity, callback: AuthorizationStateCallback?) {
        Log.i(TAG, "Got auth response: " + authorizationResponse.jsonSerializeString())
        authorizationService!!.performTokenRequest(
                authorizationResponse.createTokenExchangeRequest()
        ) { tokenResponse: TokenResponse?, ex: AuthorizationException? ->
            if (tokenResponse != null) {
                // exchange succeeded
                processTokenExchangeResponse(authorizationResponse, tokenResponse, activity)
                callback?.onAuthorizationStateUpdated()
            } else {
                // authorization failed, check ex for more details
                show(activity,
                        R.string.authorization_error_title,
                        activity.getString(R.string.authorization_error_message, ex!!.error, ex.code, ex.message))
            }
        }
    }

    /**
     * Process the the exchanged token.
     *
     * @param authorizationResponse The authorization response.
     * @param tokenResponse         The response from the token exchange updated into the authorization state
     * @param activity              The current activity.
     */
    private fun processTokenExchangeResponse(authorizationResponse: AuthorizationResponse, tokenResponse: TokenResponse, activity: Activity) {
        val authState = AuthState(authorizationResponse, tokenResponse, null)
        val accessToken = authState.accessToken
        if (accessToken == null || accessToken.isEmpty()) {
            show(activity, R.string.error_dialog_title, R.string.error_access_token_missing)
            return
        }
        // Save the authorization state.
        preferencesService.currentAuthState = authState
        // Save the access token for later use.
        historyService.cacheAuthorizationState(preferencesService.currentInstance, authState)
        val organization = preferencesService.currentOrganization
        if (organization != null) {
            historyService.storeSavedOrganization(organization)
            preferencesService.currentOrganization = null
        } else {
            Log.w(TAG, "Organization and instances were not available, so no caching was done.")
        }
    }

    /**
     * Returns a single which emits a fresh access token which is to be used with the API.
     *
     * @return The access token used to authorization in an emitter.
     */
    fun getFreshAccessToken(authState: AuthState): Single<String> {
        return if (!authState.needsTokenRefresh && authState.accessToken != null) {
            Single.just(authState.accessToken)
        } else {
            Single.create { singleEmitter: SingleEmitter<String> ->
                authState.performActionWithFreshTokens((authorizationService)!!) { accessToken, _, ex ->
                    if (accessToken != null) {
                        preferencesService.currentAuthState = authState
                        historyService.refreshAuthState(authState)
                        singleEmitter.onSuccess(accessToken)
                    } else {
                        singleEmitter.onError(ex!!)
                    }
                }
            }
        }
    }

    /**
     * Builds the authorization service configuration.
     *
     * @param discoveredAPI The discovered API URLs.
     * @return The configuration for the authorization service.
     */
    private fun buildAuthConfiguration(discoveredAPI: DiscoveredAPI): AuthorizationServiceConfiguration {
        return AuthorizationServiceConfiguration(
                Uri.parse(discoveredAPI.authorizationEndpoint),
                Uri.parse(discoveredAPI.tokenEndpoint),
                null)
    }

    interface AuthorizationStateCallback {
        fun onAuthorizationStateUpdated()
    }

    companion object {
        private val TAG = ConnectionService::class.java.name
        private const val SCOPE = BuildConfig.OAUTH_SCOPE
        private const val RESPONSE_TYPE = ResponseTypeValues.CODE
        private const val REDIRECT_URI = BuildConfig.OAUTH_REDIRECT_URI
        private const val CLIENT_ID = BuildConfig.OAUTH_CLIENT_ID
        private const val REQUEST_CODE_APP_AUTH = 100 // This is not used, since we only get one type of request for the redirect URL.
    }
}
