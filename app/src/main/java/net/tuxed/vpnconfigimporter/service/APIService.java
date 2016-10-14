package net.tuxed.vpnconfigimporter.service;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This service is responsible for fetching data from API endpoints.
 * Created by Daniel Zolnai on 2016-10-12.
 */
public class APIService {

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 20000;
    private static final int READ_BLOCK_SIZE = 16 * 1024;

    private static final String HEADER_AUTHORIZATION = "Authorization";

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
    public void getJSON(final String url, final Callback<JSONObject> callback) {
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
     * Downloads a byte array resource.
     * @param url The URL as a string.
     * @param callback The callback for notifying about the result.
     */
    public void postResource(@NonNull final String url, @Nullable final String data, final Callback<byte[]> callback) {
        AsyncTask<Void, Void, Object> asyncTask = new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... params) {
                try {
                    return _fetchByteResource(url, data);
                } catch (IOException e) {
                    return e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(Object result) {
                if (result instanceof byte[]) {
                    callback.onSuccess((byte[])result);
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
     * Downloads a byte resource from a URL.
     *
     * @param url The URL as a string.
     * @return The result as a byte array.
     * @throws IOException Thrown if there was a problem creating the connection.
     */
    private byte[] _fetchByteResource(String url, String requestData) throws IOException {
        HttpURLConnection urlConnection = _createConnection(url);
        urlConnection.setRequestMethod("POST");
        if (requestData != null) {
            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            out.write(requestData.getBytes("UTF-8"));
            out.flush();
        }
        urlConnection.connect();
        // Get the body of the response
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int bytesRead;
        byte[] data = new byte[READ_BLOCK_SIZE];
        while ((bytesRead = urlConnection.getInputStream().read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        byte[] result = buffer.toByteArray();
        int statusCode = urlConnection.getResponseCode();
        if (statusCode >= 200 && statusCode <= 299) {
            return result;
        } else {
            throw new IOException("Unsuccessful response: " + new String(result));
        }
    }

    /**
     * Creates a new URL connection based on the URL.
     *
     * @param urlString The URl as a string.
     * @return The URL connection which can be used to connect to the URL.
     * @throws IOException Thrown if there was a problem while creating the connection.
     */
    private HttpURLConnection _createConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        urlConnection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        urlConnection.setReadTimeout(READ_TIMEOUT_MS);
        urlConnection.setRequestMethod("GET");
        if (_getAccessToken() != null) {
            urlConnection.setRequestProperty(HEADER_AUTHORIZATION, "Bearer " + _getAccessToken());
        }
        return urlConnection;
    }

    /**
     * Fetches a JSON resource from a specific URL.
     *
     * @param url The URL as a string.
     * @return The JSON resource if the call was successful.
     * @throws IOException   Thrown if there was a problem while connecting.
     * @throws JSONException Thrown if the returned JSON was invalid or not a JSON at all.
     */
    private JSONObject _fetchJSON(String url) throws IOException, JSONException {
        HttpURLConnection urlConnection = _createConnection(url);
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
