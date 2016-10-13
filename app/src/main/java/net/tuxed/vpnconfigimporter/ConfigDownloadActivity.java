package net.tuxed.vpnconfigimporter;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import net.tuxed.vpnconfigimporter.service.ConnectionService;
import net.tuxed.vpnconfigimporter.service.PreferencesService;
import net.tuxed.vpnconfigimporter.utils.Downloader;
import net.tuxed.vpnconfigimporter.utils.Log;
import net.tuxed.vpnconfigimporter.utils.VpnUtils;

import java.io.IOException;
import java.io.StringReader;

import javax.inject.Inject;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ProfileManager;

public class ConfigDownloadActivity extends AppCompatActivity {

    private static final String TAG = ConfigDownloadActivity.class.getName();

    @Inject
    protected ConnectionService _connectionService;

    @Inject
    protected PreferencesService _preferencesService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EduVPNApplication.get(this).component().inject(this);
        setContentView(R.layout.activity_config_downloader);
        TextView messageTextView = (TextView)findViewById(R.id.message);
        // Start downloading the OpenVPN configuration
        String baseUrl = _preferencesService.getConnectionBaseUrl();
        String downloadURL = baseUrl + "/portal/api/config";
        //String[] taskParameters = {downloadURL, accessToken, configName};
        //DownloadFilesTask task = new DownloadFilesTask();
        //task.execute(taskParameters);
    }

    private void _importConfig(String vpnConfig, String preferredName) {

    }

    private class DownloadFilesTask extends AsyncTask<String, Void, String> {

        private String _preferredName;

        protected String doInBackground(String... s) {
            Downloader downloader = new Downloader();
            _preferredName = s[2];
            return downloader.downloadFile(s[0], s[1], s[2]);
        }

        protected void onPostExecute(String vpnConfig) {
            _importConfig(vpnConfig, _preferredName);
        }

    }
}