package net.tuxed.vpnconfigimporter.service;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This service is responsible for fetching data from API endpoints.
 * Created by Daniel Zolnai on 2016-10-12.
 */
public class APIService {

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 20000;

    private static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Callback interface for returning results asynchronously.
     */
    public interface Callback {
        /**
         * Called if the method was successful and has a result.
         *
         * @param result The result of the call.
         */
        void onSuccess(JSONObject result);

        /**
         * Called if there was a problem.
         *
         * @param errorMessage The error message as a string.
         */
        void onError(String errorMessage);
    }

    private ConnectionService _connectionService;

    public APIService(ConnectionService connectionService) {
        _connectionService = connectionService;
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
    public void getJSON(final String url, final Callback callback) {
        AsyncTask<Void, Void, Object> asyncTask = new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... params) {
                try {
                    return _fetchJSON(url);
                } catch (IOException e) {
                    return e.getMessage();
                } catch (JSONException e) {
                    return e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(Object result) {
                if (result instanceof JSONObject) {
                    callback.onSuccess((JSONObject)result);
                } else if (result instanceof String) {
                    callback.onError((String)result);
                } else {
                    throw new RuntimeException("Invalid result received from background task!");
                }
            }
        };
        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Fetches a JSON resource from a specific URL.
     *
     * @param urlString The URL as a string.
     * @return The JSON resource if the call was successful.
     * @throws IOException   Thrown if there was a problem while connecting.
     * @throws JSONException Thrown if the returned JSON was invalid or not a JSON at all.
     */
    private JSONObject _fetchJSON(String urlString) throws IOException, JSONException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        urlConnection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        urlConnection.setReadTimeout(READ_TIMEOUT_MS);
        urlConnection.setRequestMethod("GET");
        if (_getAccessToken() != null) {
            urlConnection.setRequestProperty(HEADER_AUTHORIZATION, "Bearer " + _getAccessToken());
        }
        urlConnection.connect();
        // Get the body of the response
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        bufferedReader.close();
        String responseString = stringBuilder.toString();
        int statusCode = urlConnection.getResponseCode();
        if (statusCode >= 200 && statusCode <= 299) {
            return new JSONObject(responseString);
        } else {
            throw new IOException("Unsuccessful response: " + responseString);
        }
    }
}
