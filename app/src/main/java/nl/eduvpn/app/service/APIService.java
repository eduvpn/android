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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import nl.eduvpn.app.utils.Log;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * This service is responsible for fetching data from API endpoints.
 * Created by Daniel Zolnai on 2016-10-12.
 */
public class APIService {

    public class UserNotAuthorizedException extends Exception {
    }

    private static final String TAG = APIService.class.getName();
    public static final String USER_NOT_AUTHORIZED_ERROR = "User not authorized.";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final int STATUS_CODE_UNAUTHORIZED = 401;

    /**
     * Callback interface for returning results asynchronously.
     */
    public interface Callback<T> {
        /**
         * Called if the method was successful and has a result.
         *
         * @param result The result of the call.
         */
        void onSuccess(T result);

        /**
         * Called if there was a problem.
         *
         * @param errorMessage The error message as a string.
         */
        void onError(String errorMessage);
    }

    private final ConnectionService _connectionService;
    private final OkHttpClient _okHttpClient;

    public APIService(ConnectionService connectionService, OkHttpClient okHttpClient) {
        _connectionService = connectionService;
        _okHttpClient = okHttpClient;
    }

    private String _getAccessToken() {
        return _connectionService.getAccessToken();
    }

    /**
     * Retrieves a JSON object from a URL, and returns it in the callback
     *
     * @param url      The URL to fetch the JSON from.
     * @param callback The callback for returning the result or notifying about an error.
     */
    public void getJSON(final String url, final boolean useToken, final Callback<JSONObject> callback) {
        String accessToken = _getAccessToken();
        if (!useToken) {
            accessToken = null;
        }
        final String finalToken = accessToken;
        Observable.defer(new Callable<ObservableSource<JSONObject>>() {
            @Override
            public Observable<JSONObject> call() {
                try {
                    return Observable.just(_fetchJSON(url, finalToken));
                } catch (IOException ex) {
                    return Observable.error(ex);
                } catch (JSONException ex) {
                    return Observable.error(ex);
                } catch (UserNotAuthorizedException ex) {
                    return Observable.error(ex);
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<JSONObject>() {
                    @Override
                    public void accept(JSONObject jsonObject) throws Exception {
                        callback.onSuccess(jsonObject);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (throwable instanceof UserNotAuthorizedException) {
                            callback.onError(USER_NOT_AUTHORIZED_ERROR);
                        } else {
                            callback.onError(throwable.toString());
                        }
                    }
                });
    }

    /**
     * Downloads a byte array resource.
     *
     * @param url      The URL as a string.
     * @param useToken If the authentication should be included.
     * @param data     The request data.
     * @param callback The callback for notifying about the result.
     */
    public void postResource(@NonNull final String url, @Nullable final String data, final boolean useToken, final Callback<String> callback) {
        String accessToken = _getAccessToken();
        if (!useToken) {
            accessToken = null;
        }
        final String finalToken = accessToken;
        Observable.defer(new Callable<ObservableSource<String>>() {
            @Override
            public Observable<String> call() {
                try {
                    return Observable.just(_fetchByteResource(url, data, finalToken));
                } catch (IOException ex) {
                    return Observable.error(ex);
                } catch (UserNotAuthorizedException ex) {
                    return Observable.error(ex);
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String string) throws Exception {
                        callback.onSuccess(string);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        callback.onError(throwable.toString());
                    }
                });
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
    private String _fetchByteResource(@NonNull String url, @Nullable String requestData, @Nullable String accessToken) throws IOException, UserNotAuthorizedException {
        Request.Builder requestBuilder = _createRequestBuilder(url, accessToken);
        if (requestData != null) {
            requestBuilder.method("POST", RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), requestData));
        } else {
            requestBuilder.method("POST", null);
        }
        Request request = requestBuilder.build();
        Response response = _okHttpClient.newCall(request).execute();
        int statusCode = response.code();
        if (statusCode == STATUS_CODE_UNAUTHORIZED) {
            throw new UserNotAuthorizedException();
        }
        // Get the body of the response
        String result = null;
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            result = responseBody.string();
        }
        Log.d(TAG, "POST " + url + " data: '" + requestData + "': " + result);
        if (statusCode >= 200 && statusCode <= 299) {
            return result;
        } else {
            throw new IOException("Unsuccessful response: " + result);
        }
    }

    /**
     * Creates a new URL connection based on the URL.
     *
     * @param urlString   The URL as a string.
     * @param accessToken The access token to fetch the resource with. Can be null.
     * @return The URL connection which can be used to connect to the URL.
     */
    private Request.Builder _createRequestBuilder(@NonNull String urlString, @Nullable String accessToken) {
        Request.Builder builder = new Request.Builder().get().url(urlString);
        if (accessToken != null) {
            builder = builder.header(HEADER_AUTHORIZATION, "Bearer " + accessToken);
        }
        return builder;
    }

    /**
     * Fetches a JSON resource from a specific URL.
     *
     * @param url The URL as a string.
     * @return The JSON resource if the call was successful.
     * @throws IOException   Thrown if there was a problem while connecting.
     * @throws JSONException Thrown if the returned JSON was invalid or not a JSON at all.
     */
    private JSONObject _fetchJSON(@NonNull String url, @Nullable String accessToken) throws IOException, JSONException, UserNotAuthorizedException {
        Request.Builder requestBuilder = _createRequestBuilder(url, accessToken);
        Response response = _okHttpClient.newCall(requestBuilder.build()).execute();
        int statusCode = response.code();
        if (statusCode == STATUS_CODE_UNAUTHORIZED) {
            throw new UserNotAuthorizedException();
        }
        // Get the body of the response
        String responseString = null;
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            responseString = responseBody.string();
        }
        Log.d(TAG, "GET " + url + ": " + responseString);
        if (statusCode >= 200 && statusCode <= 299) {
            return new JSONObject(responseString);
        } else {
            throw new IOException("Unsuccessful response: " + responseString);
        }
    }
}
