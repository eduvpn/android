package net.tuxed.vpnconfigimporter.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import net.tuxed.vpnconfigimporter.BuildConfig;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.InstanceList;
import net.tuxed.vpnconfigimporter.utils.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * Service which provides the app configuration.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class ConfigurationService extends Observable {

    private static final String TAG = ConfigurationService.class.getName();

    private static final String PREFERENCES = "configuration_service_preferences";
    private static final String INSTANCE_LIST_KEY = "instance_list";
    private static final String INSTANCE_LIST_ASSET = "config/default_instance_list";

    private Context _context;
    private InstanceList _instanceList;

    public ConfigurationService(Context context) {
        _context = context;
        _loadSavedInstanceList();
        _fetchLatestConfiguration();
    }

    /**
     * Returns the instance list configuration.
     *
     * @return The instance list configuration.
     */
    public InstanceList getInstanceList() {
        return _instanceList;
    }

    private SharedPreferences _getPreferences() {
        return _context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    private void _loadSavedInstanceList() {
        // Loads the saved configuration from the storage.
        // If none found, it will default to the one in the app.
        String savedInstanceList = _getPreferences().getString(INSTANCE_LIST_KEY, null);
        try {
            if (savedInstanceList != null) {
                _instanceList = _parseInstanceList(savedInstanceList);
                return;
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Unable to parse saved instance list JSON. Loading app static instance list.", ex);
        }
        // No saved instance list, or error while parsing.
        // Load hardcoded backup.
        String hardcodedInstanceList;
        try {
            InputStream instanceListStream = _context.getAssets().open(INSTANCE_LIST_ASSET);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(instanceListStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            hardcodedInstanceList = stringBuilder.toString();
            _instanceList = _parseInstanceList(hardcodedInstanceList);
            _saveInstanceList();
        } catch (IOException | JSONException ex) {
            throw new RuntimeException("Error reading default asset file!", ex);
        }
    }

    /**
     * Saves the currently loaded instance list so it can be loaded next time.
     */
    private void _saveInstanceList() {
        try {
            if (_instanceList == null) {
                throw new RuntimeException("No instance list set!");
            }
            JSONObject serialized = new JSONObject();
            serialized.put("list_version", _instanceList.getVersion());
            JSONArray serializedInstances = new JSONArray();
            for (Instance instance : _instanceList.getInstanceList()) {
                JSONObject serializedInstance = new JSONObject();
                serializedInstance.put("base_uri", instance.getBaseUri());
                serializedInstance.put("display_name", instance.getDisplayName());
                serializedInstance.put("logo_uri", instance.getLogoUri());
                serializedInstances.put(serializedInstance);
            }
            serialized.put("instances", serializedInstances);
            _getPreferences().edit().putString(INSTANCE_LIST_KEY, serialized.toString()).apply();
        } catch (JSONException ex) {
            Log.e(TAG, "Unable to save the instance list!", ex);
        }

    }

    /**
     * Parses the JSON string of the instance list to a POJO object.
     *
     * @param instanceListString The string with the JSON representation.
     * @return An InstanceList object containing the same information.
     * @throws JSONException Thrown if the JSON was malformed or had an unknown list version.
     */
    private InstanceList _parseInstanceList(String instanceListString) throws JSONException {
        JSONObject instanceListJson = new JSONObject(instanceListString);
        Integer listVersion = instanceListJson.getInt("list_version");
        if (listVersion != 1) {
            throw new JSONException("Unknown list_version property: " + listVersion);
        }
        JSONArray instanceArray = instanceListJson.getJSONArray("instances");
        List<Instance> instances = new ArrayList<>();
        for (int i = 0; i < instanceArray.length(); ++i) {
            JSONObject instanceObject = instanceArray.getJSONObject(i);
            String baseUri = instanceObject.getString("base_uri");
            String displayName = instanceObject.getString("display_name");
            String logoUri = null;
            if (instanceObject.has("logo_uri")) {
                logoUri = instanceObject.getString("logo_uri");
            }
            instances.add(new Instance(baseUri, displayName, logoUri));
        }
        return new InstanceList(listVersion, instances);
    }

    /**
     * Downloads, parses, and saves the latest configuration retrieved from the URL defined in the build configuration.
     */
    private void _fetchLatestConfiguration() {
        AsyncTask<Void, Void, InstanceList> downloadTask = new AsyncTask<Void, Void, InstanceList>() {
            @Override
            protected InstanceList doInBackground(Void... params) {
                try {
                    URL url = new URL(BuildConfig.INSTANCE_LIST_URL);
                    HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setConnectTimeout(10000);
                    urlConnection.setReadTimeout(10000);
                    urlConnection.connect();
                    InputStream inputStream = urlConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append('\n');
                    }
                    String instanceList = stringBuilder.toString();
                    return  _parseInstanceList(instanceList);
                } catch (IOException | JSONException ex) {
                    Log.w(TAG, "Error reading latest configuration from the URL!", ex);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(InstanceList instanceList) {
                if (instanceList != null) {
                    _instanceList = instanceList;
                    _saveInstanceList();
                    setChanged();
                    notifyObservers();
                }
            }
        };
        downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


}
