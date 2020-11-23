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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import nl.eduvpn.app.utils.Log
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * This service is responsible for fetching data from API endpoints.
 * Created by Daniel Zolnai on 2016-10-12.
 */
class APIService(private val connectionService: ConnectionService, private val okHttpClient: OkHttpClient) {

    class UserNotAuthorizedException : Exception()

    /**
     * Callback interface for returning results asynchronously.
     */
    interface Callback<T> {
        /**
         * Called if the method was successful and has a result.
         *
         * @param result The result of the call.
         */
        fun onSuccess(result: T)

        /**
         * Called if there was a problem.
         *
         * @param errorMessage The error message as a string.
         */
        fun onError(errorMessage: String)
    }

    /**
     * Retrieves a JSON object from a URL, and returns it in the callback
     *
     * @param url      The URL to fetch the JSON from.
     * @param callback The callback for returning the result or notifying about an error.
     */
    fun getJSON(url: String, authState: AuthState?, callback: Callback<JSONObject>) {
        getString(url, authState, object : Callback<String> {
            override fun onSuccess(result: String) {
                try {
                    callback.onSuccess(JSONObject(result))
                } catch (ex: JSONException) {
                    callback.onError("Error parsing JSON: $ex")
                }
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    /**
     * Retrieves a resource as a string.
     *
     * @param url      The URL to get the resource from.
     * @param authState If the access token should be used, provide a previous authorization state.
     * @param callback The callback where the result is returned.
     */
    fun getString(url: String, authState: AuthState?, callback: Callback<String>) {
        GlobalScope.launch(Dispatchers.Main) {
            kotlin.runCatching {
                createNetworkCall(authState) { accessToken ->
                    fetchString(url, accessToken)
                }
            }.mapCatching { result ->
                if (result == null) {
                    throw RuntimeException("Unexpected result type!")
                }
                result
            }.onSuccess { result ->
                callback.onSuccess(result)
            }.onFailure { throwable ->
                if (throwable is UserNotAuthorizedException) {
                    callback.onError(USER_NOT_AUTHORIZED_ERROR)
                } else {
                    callback.onError(throwable.toString())
                }
            }
        }
    }

    /**
     * Downloads a byte array resource.
     *
     * @param url      The URL as a string.
     * @param authState If an auth token should be sent, include an auth state.
     * @param data     The request data.
     * @param callback The callback for notifying about the result.
     */
    fun postResource(url: String, data: String?, authState: AuthState?, callback: Callback<String>) {
        GlobalScope.launch(Dispatchers.Main) {
            kotlin.runCatching {
                createNetworkCall(authState) { accessToken ->
                    fetchByteResource(url, data, accessToken)
                }
            }.mapCatching { result ->
                if (result == null) {
                    throw RuntimeException("Unexpected result type!")
                }
                result
            }.onSuccess { result ->
                callback.onSuccess(result)
            }.onFailure { throwable ->
                callback.onError(throwable.toString())
            }
        }
    }

    /**
     * Downloads a byte resource from a URL.
     *
     * @param url         The URL as a string.
     * @param requestData The request data, if any.
     * @param accessToken The access token to fetch the resource with. Can be null.
     * @return The result as a byte array.
     * @throws IOException Thrown if there was a problem creating the connection.
     */
    @Throws(IOException::class, UserNotAuthorizedException::class)
    private suspend fun fetchByteResource(url: String, requestData: String?, accessToken: String?): String? {
        val requestBuilder = createRequestBuilder(url, accessToken)
        if (requestData != null) {
            requestBuilder.method("POST", requestData.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()))
        } else {
            requestBuilder.method("POST", null)
        }
        val request = requestBuilder.build()
        return withContext(Dispatchers.IO) {
            suspendCoroutine { cont ->
                okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        cont.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val statusCode = response.code
                        if (statusCode == STATUS_CODE_UNAUTHORIZED) {
                            cont.resumeWithException(UserNotAuthorizedException())
                        }
                        // Get the body of the response
                        val responseBody = response.body
                        val result = responseBody?.string()
                        responseBody?.close()
                        Log.d(TAG, "POST $url data: '$requestData': $result")
                        return if (statusCode in 200..299) {
                            cont.resume(result)
                        } else {
                            cont.resumeWithException(IOException("Unsuccessful response: $result"))
                        }
                    }
                })
            }
        }
    }

    /**
     * Creates a new URL connection based on the URL.
     *
     * @param urlString   The URL as a string.
     * @param accessToken The access token to fetch the resource with. Can be null.
     * @return The URL connection which can be used to connect to the URL.
     */
    private fun createRequestBuilder(urlString: String, accessToken: String?): Request.Builder {
        var builder = Request.Builder().get().url(urlString)
        if (accessToken != null && accessToken.isNotEmpty()) {
            builder = builder.header(HEADER_AUTHORIZATION, "Bearer $accessToken")
        }
        return builder
    }

    /**
     * Fetches a JSON resource from a specific URL.
     *
     * @param url The URL as a string.
     * @return The JSON resource if the call was successful.
     * @throws IOException   Thrown if there was a problem while connecting.
     * @throws JSONException Thrown if the returned JSON was invalid or not a JSON at all.
     */
    @Throws(IOException::class, JSONException::class, UserNotAuthorizedException::class)
    private suspend fun fetchString(url: String, accessToken: String?): String? {
        val requestBuilder = createRequestBuilder(url, accessToken)
        return withContext(Dispatchers.IO) {
            suspendCoroutine { cont ->
                okHttpClient.newCall(requestBuilder.build()).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        cont.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val statusCode = response.code
                        if (statusCode == STATUS_CODE_UNAUTHORIZED) {
                            cont.resumeWithException(UserNotAuthorizedException())
                        }
                        // Get the body of the response
                        val responseBody = response.body
                        val responseString = responseBody?.string()
                        responseBody?.close()
                        Log.d(TAG, "GET $url: $responseString")
                        cont.resume(responseString)
                    }
                })
            }
        }
    }

    private suspend fun <R> createNetworkCall(authState: AuthState?, networkFunction: suspend (String) -> R): R {
        return if (authState != null) {
            networkFunction(connectionService.getFreshAccessToken(authState))
        } else {
            networkFunction("")
        }
    }

    companion object {
        private val TAG = APIService::class.java.name
        const val USER_NOT_AUTHORIZED_ERROR = "User not authorized."
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val STATUS_CODE_UNAUTHORIZED = 401
    }
}
