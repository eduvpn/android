package net.tuxed.vpnconfigimporter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import net.tuxed.vpnconfigimporter.utils.Downloader;
import net.tuxed.vpnconfigimporter.utils.Log;
import net.tuxed.vpnconfigimporter.utils.VpnUtils;

import java.io.IOException;
import java.io.StringReader;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ProfileManager;

public class ConfigDownloadActivity extends AppCompatActivity {

    private static final String TAG = ConfigDownloadActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_downloader);
        TextView messageTextView = (TextView)findViewById(R.id.message);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        Log.i(TAG, "Callback URL: " + uri.toString());

        String fragment = uri.getFragment();

        String[] fragments = fragment.split("&");

        String accessToken = null;
        String state = null;

        for (String element : fragments) {
            String[] keyValuePair = element.split("=");
            if (keyValuePair[0].equals("access_token")) {
                // Found access token
                accessToken = keyValuePair[1];
            }
            if (keyValuePair[0].equals("state")) {
                // Found state
                state = keyValuePair[1];
            }

        }

        if (accessToken == null) {
            messageTextView.setText(R.string.error_access_token_missing);
            return;
        }
        if (state == null) {
            messageTextView.setText(R.string.error_state_missing);
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.APPLICATION_PREFERENCES, Context.MODE_PRIVATE);
        String savedState = sharedPreferences.getString(Constants.KEY_STATE, null);

        if (savedState == null || !savedState.equals(state)) {
            messageTextView.setText(R.string.error_state_mismatch);
            return;
        }

        // Now we can delete the saved state
        sharedPreferences.edit().remove(Constants.KEY_STATE).apply();

        // Start downloading the OpenVPN configuration
        String configName = "Android_" + System.currentTimeMillis() / 1000L;
        String vpnHost = sharedPreferences.getString("host", null);
        String downloadURL = "https://" + vpnHost + "/portal/api/config";
        String[] taskParameters = {downloadURL, accessToken, configName};
        DownloadFilesTask task = new DownloadFilesTask();
        task.execute(taskParameters);
    }

    private void _importConfig(String vpnConfig) {
        ConfigParser configParser = new ConfigParser();
        try {
            configParser.parseConfig(new StringReader(vpnConfig));
            VpnProfile profile = configParser.convertProfile();
            ProfileManager profileManager = ProfileManager.getInstance(ConfigDownloadActivity.this);
            profileManager.addProfile(profile);
            profileManager.saveProfile(ConfigDownloadActivity.this, profile);
            Log.i(TAG, "Added and saved profile with UUID: " + profile.getUUIDString());
            VpnUtils.startConnectionWithProfile(ConfigDownloadActivity.this, profile);
        } catch (IOException | ConfigParser.ConfigParseError e) {
            Log.e(TAG, "Error converting profile!", e);
        }
    }

    private class DownloadFilesTask extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... s) {
            Downloader downloader = new Downloader();
            return downloader.downloadFile(s[0], s[1], s[2]);
        }

        protected void onPostExecute(String vpnConfig) {
            _importConfig(vpnConfig);
        }

    }
}