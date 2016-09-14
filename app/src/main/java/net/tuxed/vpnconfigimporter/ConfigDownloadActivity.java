package net.tuxed.vpnconfigimporter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

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
        setContentView(R.layout.activity_register);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        Log.i(TAG, uri.toString());

        String fragment = uri.getFragment();

        String[] fragments = fragment.split("&");

        String accessToken = null;
        String state = null;

        for (String element : fragments) {
            String[] kv = element.split("=");
            if (kv[0].equals("access_token")) {
                // found access token
                accessToken = kv[1];
            }
            if (kv[0].equals("state")) {
                // found access token
                state = kv[1];
            }

        }
        boolean error = false;

        TextView t = (TextView)findViewById(R.id.textView);

        if (null == accessToken) {
            t.setText("accessToken not found in callback URL");
            error = true;

        }
        if (null == state) {
            t.setText("state not found in callback URL");
            error = true;

        }
        SharedPreferences settings = getSharedPreferences("vpn-state", 0);

        String settingsState = settings.getString("state", "x");    // FIXME, die when no state stored

        if (!state.equals(settingsState)) {
            t.setText("state does not match state we sent");
            error = true;
        }

        //FIXME delete state / URL from settings

        if (!error) {
            String configName = "Android_" + System.currentTimeMillis() / 1000L;
            String vpnHost = settings.getString("host", null);
            String newU = "https://" + vpnHost + "/portal/api/config";
            String[] s = {newU, accessToken, configName};
            DownloadFilesTask d = new DownloadFilesTask();
            d.execute(s);
        }
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