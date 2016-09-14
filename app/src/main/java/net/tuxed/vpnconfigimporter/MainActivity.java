package net.tuxed.vpnconfigimporter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import java.util.Iterator;
import java.util.UUID;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // If there's a saved config available, we give the option to connect with it
        final ProfileManager profileManager = ProfileManager.getInstance(MainActivity.this);
        if (profileManager.getProfiles() != null && profileManager.getProfiles().size() > 0) {
            // There's a saved profile
            findViewById(R.id.savedConfigAvailable).setVisibility(View.VISIBLE);
            findViewById(R.id.requestNewConfigText).setVisibility(View.VISIBLE);
            Button connectButton = (Button)findViewById(R.id.connectButton);
            connectButton.setVisibility(View.VISIBLE);
            connectButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    _connectWithProfile(profileManager.getProfiles().iterator().next());
                }
            });
        }

        Button startSetup = (Button)findViewById(R.id.setupButton);
        startSetup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                _startSetup();
            }
        });
    }

    private void _connectWithProfile(VpnProfile vpnProfile) {
        Log.i(TAG, String.format("Initiating connection with profile '%s'", vpnProfile.getUUIDString()));
        VpnUtils.startConnectionWithProfile(MainActivity.this, vpnProfile);
    }

    private void _startSetup() {
        // Remove all saved profiles first
        final ProfileManager profileManager = ProfileManager.getInstance(MainActivity.this);
        if (profileManager.getProfiles() != null && profileManager.getProfiles().size() > 0) {
            Iterator<VpnProfile> profileIterator = profileManager.getProfiles().iterator();
            while (profileIterator.hasNext()) {
                profileIterator.remove();
            }
        }
        // Generate the auth URL
        String state = UUID.randomUUID().toString();
        SharedPreferences settings = getSharedPreferences("vpn-state", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("state", state);

        EditText vpnUrl = (EditText)findViewById(R.id.vpnUrl);

        String vpnHost = vpnUrl.getText().toString();
        editor.putString("host", vpnHost);
        editor.apply();

        String openUrl = "https://" + vpnHost + "/portal/_oauth/authorize?client_id=vpn-companion&redirect_uri=vpn://import/callback&response_type=token&scope=create_config&state=" + state;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(openUrl));
        startActivity(i);
    }
}
