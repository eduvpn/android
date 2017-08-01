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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import nl.eduvpn.app.BuildConfig;
import nl.eduvpn.app.entity.ConnectionType;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.InstanceList;
import nl.eduvpn.app.utils.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Service which provides the app configuration.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class ConfigurationService extends Observable {

    private static final String TAG = ConfigurationService.class.getName();

    private static final String PREFERENCES = "configuration_service_preferences";
    private static final String INSTANCE_LIST_KEY = "instance_list";
    private static final String FEDERATION_LIST_KEY = "federation_list";


    private Context _context;
    private SerializerService _serializerService;
    private OkHttpClient _okHttpClient;

    private InstanceList _instanceList;
    private InstanceList _federationList;

    public ConfigurationService(Context context, SerializerService serializerService, OkHttpClient okHttpClient) {
        _context = context;
        _serializerService = serializerService;
        _okHttpClient = okHttpClient;
        _loadSavedLists();
        _fetchLatestConfiguration();
    }

    /**
     * Returns the instance list configuration.
     *
     * @return The instance list configuration.
     */
    @NonNull
    public List<Instance> getInstanceList() {
        if (_instanceList == null) {
            return Collections.emptyList();
        } else {
            return _instanceList.getInstanceList();
        }
    }

    @NonNull
    public List<Instance> getFederationList() {
        if (_federationList == null) {
            return Collections.emptyList();
        } else {
            return _federationList.getInstanceList();
        }
    }

    private SharedPreferences _getPreferences() {
        return _context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    private void _loadSavedLists() {
        // Loads the saved configuration from the storage.
        // If none found, it will default to the one in the app.
        String savedInstanceList = _getPreferences().getString(INSTANCE_LIST_KEY, null);
        String savedFederationList = _getPreferences().getString(FEDERATION_LIST_KEY, null);
        try {
            if (savedInstanceList != null) {
                _instanceList = _parseInstanceList(savedInstanceList);
            }
            if (savedFederationList != null) {
                _federationList = _parseInstanceList(savedFederationList);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unable to parse saved instance list JSON.", ex);
        }
    }

    /**
     * Saves the currently loaded instance list so it can be loaded next time.
     */
    private void _saveLists() {
        try {
            if (_instanceList == null) {
                throw new RuntimeException("No instance list set!");
            }
            JSONObject serialized = _serializerService.serializeInstanceList(_instanceList);
            _getPreferences().edit().putString(INSTANCE_LIST_KEY, serialized.toString()).apply();
            if (_federationList == null) {
                throw new RuntimeException("No federation list set!");
            }
            serialized = _serializerService.serializeInstanceList(_federationList);
            _getPreferences().edit().putString(FEDERATION_LIST_KEY, serialized.toString()).apply();
        } catch (SerializerService.UnknownFormatException ex) {
            Log.e(TAG, "Unable to save the instance or federation list!", ex);
        }

    }

    /**
     * Parses the JSON string of the instance list to a POJO object.
     *
     * @param instanceListString The string with the JSON representation.
     * @return An InstanceList object containing the same information.
     * @throws JSONException Thrown if the JSON was malformed or had an unknown list version.
     */
    private InstanceList _parseInstanceList(String instanceListString) throws Exception {
        JSONObject instanceListJson = new JSONObject(instanceListString);
        return _serializerService.deserializeInstanceList(instanceListJson);
    }

    /**
     * Downloads, parses, and saves the latest configuration retrieved from the URL defined in the build configuration.
     */
    private void _fetchLatestConfiguration() {
        AsyncTask<Integer, Void, TaskResult> instituteAccessTask = new DownloadTask();
        AsyncTask<Integer, Void, TaskResult> secureInternetTask = new DownloadTask();
        instituteAccessTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ConnectionType.INSTITUTE_ACCESS);
        secureInternetTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ConnectionType.SECURE_INTERNET);
    }

    private class DownloadTask extends AsyncTask<Integer, Void, TaskResult> {

        @Override
        protected TaskResult doInBackground(Integer... params) {
            try {
                // First fetch the instance list
                int connectionType = params[0];
                String requestUrl = connectionType == ConnectionType.SECURE_INTERNET ? BuildConfig.INSTANCE_LIST_URL : BuildConfig.FEDERATION_LIST_URL;
                Request request = new Request.Builder().url(requestUrl).build();
                Response response = _okHttpClient.newCall(request).execute();
                if (response.body() != null) {
                    //noinspection WrongConstant
                    return TaskResult.success(connectionType, _parseInstanceList(response.body().string()));
                } else {
                    throw new IOException("Response body is empty!");
                }

            } catch (Exception ex) {
                Log.w(TAG, "Error reading latest configuration from the URL!", ex);
                return TaskResult.fail();
            }
        }

        @Override
        protected void onPostExecute(TaskResult taskResult) {
            if (taskResult.isSuccessful()) {
                if (taskResult.getConnectionType() == ConnectionType.INSTITUTE_ACCESS) {
                    _federationList = taskResult.getInstanceList();
                } else {
                    _instanceList = taskResult.getInstanceList();
                }
                _saveLists();
                setChanged();
                notifyObservers();
            }
        }
    };

    private static class TaskResult {
        @ConnectionType
        private int _connectionType;

        private InstanceList _instanceList;

        private boolean _successful;

        private TaskResult(boolean successful, int connectionType, InstanceList instanceList) {
            _successful = successful;
            _connectionType = connectionType;
            _instanceList = instanceList;
        }

        public static TaskResult fail() {
            return new TaskResult(false, -1, null);
        }

        public static TaskResult success(@ConnectionType int connectionType, InstanceList result) {
            return new TaskResult(true, connectionType, result);
        }

        public boolean isSuccessful() {
            return _successful;
        }

        public InstanceList getInstanceList() {
            return _instanceList;
        }

        @ConnectionType
        public int getConnectionType() {
            return _connectionType;
        }
    }

}
