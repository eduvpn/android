package net.tuxed.vpnconfigimporter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import net.tuxed.vpnconfigimporter.utils.VpnUtils;

import java.util.Iterator;
import java.util.UUID;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity implements VpnStatus.StateListener {

    private static final String TAG = MainActivity.class.getName();

    private enum AppStatus {
        DISCONNECTED_SAVED_CONFIG,
        DISCONNECTED_NO_CONFIG,
        CONNECTED
    }

    private Handler _uiHandler = new Handler();
    private VpnStatus.ConnectionStatus _connectionStatus;
    private OpenVPNService _openVPNService;
    private ServiceConnection _serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            OpenVPNService.LocalBinder binder = (OpenVPNService.LocalBinder)service;
            _openVPNService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            _openVPNService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        VpnStatus.addStateListener(this);
        Intent intent = new Intent(this, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        bindService(intent, _serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(_serviceConnection);
        VpnStatus.removeStateListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Button startSetup = (Button)findViewById(R.id.setupButton);
        startSetup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                _startSetup();
            }
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
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
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.APPLICATION_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.KEY_STATE, state);

        EditText vpnUrl = (EditText)findViewById(R.id.vpnUrl);

        String vpnHost = vpnUrl.getText().toString();
        editor.putString(Constants.KEY_HOST, vpnHost);
        editor.apply();

        String openUrl = "https://" + vpnHost + "/portal/_oauth/authorize?client_id=vpn-companion&redirect_uri=vpn://import/callback&response_type=token&scope=create_config&state=" + state;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(openUrl));
        startActivity(intent);
    }

    @Override
    public void updateState(final String state, String logmessage, int localizedResId, VpnStatus.ConnectionStatus level) {
        _connectionStatus = level;
        _uiHandler.post(new Runnable() {
            @Override
            public void run() {
                TextView statusView = (TextView)findViewById(R.id.currentStatus);
                if (statusView != null) {
                    statusView.setText(getString(R.string.current_status, state));
                }
                if (_connectionStatus == VpnStatus.ConnectionStatus.LEVEL_CONNECTED) {
                    onAppStatusChanged(AppStatus.CONNECTED);
                } else {
                    boolean hasSavedProfile = false;
                    final ProfileManager profileManager = ProfileManager.getInstance(MainActivity.this);
                    if (profileManager.getProfiles() != null && profileManager.getProfiles().size() > 0) {
                        hasSavedProfile = true;
                    }
                    onAppStatusChanged(hasSavedProfile ? AppStatus.DISCONNECTED_SAVED_CONFIG : AppStatus.DISCONNECTED_NO_CONFIG);
                }
            }
        });

    }

    public void onAppStatusChanged(AppStatus appStatus) {
        if (appStatus == AppStatus.DISCONNECTED_NO_CONFIG) {
            findViewById(R.id.savedConfigAvailable).setVisibility(View.GONE);
            findViewById(R.id.requestNewConfigText).setVisibility(View.GONE);
            Button connectButton = (Button)findViewById(R.id.connectButton);
            connectButton.setText(R.string.button_start_connection);
            connectButton.setVisibility(View.GONE);
        } else if (appStatus == AppStatus.DISCONNECTED_SAVED_CONFIG) {
            findViewById(R.id.savedConfigAvailable).setVisibility(View.VISIBLE);
            findViewById(R.id.requestNewConfigText).setVisibility(View.VISIBLE);
            Button connectButton = (Button)findViewById(R.id.connectButton);
            connectButton.setVisibility(View.VISIBLE);
            connectButton.setText(R.string.button_start_connection);
            final ProfileManager profileManager = ProfileManager.getInstance(MainActivity.this);
            connectButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    _connectWithProfile(profileManager.getProfiles().iterator().next());
                }
            });
        } else if (appStatus == AppStatus.CONNECTED) {
            Button disconnectButton = (Button)findViewById(R.id.connectButton);
            disconnectButton.setText(R.string.button_disconnect);
            disconnectButton.setVisibility(View.VISIBLE);
            findViewById(R.id.requestNewConfigText).setVisibility(View.VISIBLE);
            disconnectButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    _openVPNService.getManagement().stopVPN(false);
                }
            });
            TextView savedConfigAvailable = (TextView)findViewById(R.id.savedConfigAvailable);
            savedConfigAvailable.setVisibility(View.GONE);
        }
    }
}
