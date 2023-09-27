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
import android.os.Build
import android.os.Parcelable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.*
import net.openid.appauth.browser.BrowserDenyList
import net.openid.appauth.browser.VersionedBrowserMatcher
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.DiscoveredAPI
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.exception.EduVPNException
import nl.eduvpn.app.utils.ErrorDialog.show
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.runCatchingCoroutine
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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

    fun onStart(activity: Activity) {
        authorizationService = if (!preferencesService.getAppSettings().useCustomTabs()) {
            // We do not allow any custom tab implementation.
            AuthorizationService(activity, AppAuthConfiguration.Builder()
                    .setBrowserMatcher(BrowserDenyList(
                            VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                            VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB)
                        )
                    .build())
        } else {
            // Default behavior
            AuthorizationService(activity)
        }
    }

    fun onStop() {
        authorizationService?.dispose()
        authorizationService = null
    }

    /**
     * Initiates a connection to the VPN provider instance.
     *
     * @param activity      The current activity.
     * @param instance      The instance to connect to.
     * @param discoveredAPI The discovered API which has the URL.
     */
    suspend fun initiateConnection(activity: Activity, instance: Instance, authStringToOpen: String) {
        withContext(Dispatchers.Main) {
            runCatchingCoroutine {
                val stateString = securityService.generateSecureRandomString(32)
                preferencesService.setCurrentInstance(instance)
                val serviceConfig = buildAuthConfiguration(authStringToOpen)
                val uriToOpen = Uri.parse(authStringToOpen)
                val redirectUri = uriToOpen.getQueryParameter("redirect_uri")
                val authRequestBuilder = AuthorizationRequest.Builder(
                    serviceConfig,  // the authorization service configuration
                    CLIENT_ID,  // the client ID, typically pre-registered and static
                    ResponseTypeValues.CODE,  // the response_type value: we want a code
                        Uri.parse(redirectUri)) // the redirect URI to which the auth response is sent
                    .setScope(SCOPE)
                    .setCodeVerifier(CodeVerifierUtil.generateRandomCodeVerifier()) // Use S256 challenge method if possible
                    .setResponseType(RESPONSE_TYPE)
                    .setState(stateString)
                authRequestBuilder.build()
            }.onSuccess { authorizationRequest ->
                if (authorizationService == null) {
                    throw RuntimeException("Please call onStart() on this service from your activity!")
                }
                if (!activity.isFinishing) {
                    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_MUTABLE
                    } else {
                        0
                    }
                    val originalIntent = authorizationService!!.getAuthorizationRequestIntent(
                        authorizationRequest,
                        PendingIntent.getActivity(
                            activity,
                            REQUEST_CODE_APP_AUTH,
                            Intent(activity, MainActivity::class.java),
                            flags
                        )
                    )
                    if (instance.authenticationUrlTemplate != null
                        && instance.authenticationUrlTemplate.isNotEmpty()
                        //todo: fix deprecation when new compat library released https://issuetracker.google.com/issues/242048899
                        && @Suppress("DEPRECATION") originalIntent.getParcelableExtra<Parcelable?>("authIntent") != null
                        && preferencesService.getCurrentOrganization() != null
                    ) {
                        //todo: fix deprecation when new compat library released https://issuetracker.google.com/issues/242048899
                        @Suppress("DEPRECATION") val authIntent =
                            originalIntent.getParcelableExtra<Intent>("authIntent")
                        if (authIntent != null && authIntent.dataString != null) {
                            val replacedUrl = instance.authenticationUrlTemplate
                                .replace("@RETURN_TO@", Uri.encode(authIntent.dataString))
                                .replace(
                                    "@ORG_ID@",
                                    Uri.encode(preferencesService.getCurrentOrganization()!!.orgId)
                                )
                            authIntent.data = Uri.parse(replacedUrl)
                            originalIntent.putExtra("authIntent", authIntent)
                        }
                    }
                    activity.startActivity(originalIntent)
                }
            }.onFailure { throwable ->
                if (!activity.isFinishing) {
                    show(activity, R.string.error_dialog_title, (throwable.message)!!)
                }
            }
        }
    }

    /**
     * Parses the authorization response and retrieves the access token.
     *
     * @param authorizationResponse The auth response to process.
     */
    suspend fun parseAuthorizationResponse(
        authorizationResponse: AuthorizationResponse,
        authenticationDate: Date
    ): Result<Unit> {
        Log.i(TAG, "Got auth response: " + authorizationResponse.jsonSerializeString())
        val tokenResponse = suspendCoroutine<Result<TokenResponse>> { cont ->
            authorizationService!!.performTokenRequest(
                authorizationResponse.createTokenExchangeRequest()
            ) { tokenResponse: TokenResponse?, ex: AuthorizationException? ->
                if (tokenResponse != null) {
                    cont.resume(Result.success(tokenResponse))
                } else {
                    cont.resume(
                        Result.failure(
                            EduVPNException(
                                R.string.authorization_error_title,
                                R.string.authorization_error_message,
                                ex!!.error,
                                ex.code,
                                ex.message
                            )
                        )
                    )
                }
            }
        }.getOrElse { return Result.failure(it) }

        return processTokenExchangeResponse(
            authorizationResponse,
            tokenResponse,
            authenticationDate
        )
    }

    /**
     * Process the the exchanged token.
     *
     * @param authorizationResponse The authorization response.
     * @param tokenResponse         The response from the token exchange updated into the authorization state
     */
    private fun processTokenExchangeResponse(
        authorizationResponse: AuthorizationResponse,
        tokenResponse: TokenResponse,
        authenticationDate: Date,
    ): Result<Unit> {
        val authState = AuthState(authorizationResponse, tokenResponse, null)
        val accessToken = authState.accessToken
        if (accessToken == null || accessToken.isEmpty()) {
            return Result.failure(
                EduVPNException(
                    R.string.error_dialog_title,
                    R.string.error_access_token_missing
                )
            )
        }
        // Save the authorization state.
        preferencesService.setCurrentAuthState(authState)
        // Save the access token for later use.
        historyService.cacheAuthorizationState(
            preferencesService.getCurrentInstance()!!,
            authState,
            authenticationDate
        )
        val organization = preferencesService.getCurrentOrganization()
        if (organization != null) {
            historyService.storeSavedOrganization(organization)
            preferencesService.setCurrentOrganization(null)
        } else {
            Log.w(TAG, "Organization and instances were not available, so no caching was done.")
        }
        return Result.success((Unit))
    }

    /**
     * Returns a single which emits a fresh access token which is to be used with the API.
     *
     * @return The access token used to authorization in an emitter.
     */
    suspend fun getFreshAccessToken(authState: AuthState): String {
        val accessToken = authState.accessToken
        return if (!authState.needsTokenRefresh && accessToken != null) {
            accessToken
        } else {
            withContext(Dispatchers.IO) {
                suspendCoroutine<String> { cont ->
                    authState.performActionWithFreshTokens((authorizationService)!!) { accessToken, _, ex ->
                        if (accessToken != null) {
                            preferencesService.setCurrentAuthState(authState)
                            historyService.refreshAuthState(authState)
                            cont.resume(accessToken)
                        } else {
                            cont.resumeWithException(ex!!)
                        }
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
    private fun buildAuthConfiguration(authStringToOpen: String): AuthorizationServiceConfiguration {
        return AuthorizationServiceConfiguration(
                Uri.parse(authStringToOpen),
                Uri.parse(authStringToOpen),
                null)
    }

    companion object {
        private val TAG = ConnectionService::class.java.name
        private const val SCOPE = BuildConfig.OAUTH_SCOPE
        private const val RESPONSE_TYPE = ResponseTypeValues.CODE
        private const val CLIENT_ID = BuildConfig.OAUTH_CLIENT_ID
        private const val REQUEST_CODE_APP_AUTH = 100 // This is not used, since we only get one type of request for the redirect URL.
    }
}
