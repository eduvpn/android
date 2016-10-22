package net.tuxed.vpnconfigimporter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import net.tuxed.vpnconfigimporter.fragment.ConnectionStatusFragment;
import net.tuxed.vpnconfigimporter.fragment.HomeFragment;
import net.tuxed.vpnconfigimporter.service.ConnectionService;
import net.tuxed.vpnconfigimporter.service.VPNService;
import net.tuxed.vpnconfigimporter.utils.ErrorDialog;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    @Inject
    protected ConnectionService _connectionService;

    @Inject
    protected VPNService _vpnService;

    @BindView(R.id.toolbar)
    protected Toolbar _toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        EduVPNApplication.get(this).component().inject(this);
        setSupportActionBar(_toolbar);
        _vpnService.onCreate(this);
        if (savedInstanceState == null) {
            // If there's an ongoing VPN connection, open the status screen.
            if (_vpnService.getStatus() != VPNService.VPNStatus.DISCONNECTED) {
                openFragment(new ConnectionStatusFragment(), false);
            } else {
                // Else we just show the provider selection fragment.
                openFragment(new HomeFragment(), false);
            }
        } // else the activity will automatically restore everything.
        // The app might have been reopened from a URL.
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getData() == null) {
            // Not a callback intent.
            return;
        }
        if (_vpnService.getStatus() != VPNService.VPNStatus.DISCONNECTED) {
            // The user clicked on an authorization link while the VPN is connected.
            // Maybe just a mistake?
            Toast.makeText(this, R.string.already_connected_please_disconnect, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            _connectionService.parseCallbackIntent(intent);
            // Remove it so we don't parse it again.
            intent.setData(null);
            // Show the home fragment, so the user can select his new config(s)
            openFragment(new HomeFragment(), false);
            Toast.makeText(this, R.string.provider_added_new_configs_available, Toast.LENGTH_LONG).show();
        } catch (ConnectionService.InvalidConnectionAttemptException ex) {
            ErrorDialog.show(this, R.string.error_dialog_title, ex.getMessage());
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        _vpnService.onDestroy(this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void openFragment(Fragment fragment, boolean openOnTop) {
        if (openOnTop) {
            getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .add(R.id.contentFrame, fragment)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contentFrame, fragment)
                    .commit();
        }
    }

    @OnClick(R.id.settingsButton)
    protected void onSettingsButtonClicked() {
        findViewById(R.id.settingsButton).animate().rotationBy(520).setDuration(800).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        Toast.makeText(this, "This will open the settings page later...", Toast.LENGTH_LONG).show();
    }


}
