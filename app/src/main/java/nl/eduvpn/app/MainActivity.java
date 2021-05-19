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

package nl.eduvpn.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;

import javax.inject.Inject;

import nl.eduvpn.app.base.BaseActivity;
import nl.eduvpn.app.databinding.ActivityMainBinding;
import nl.eduvpn.app.fragment.AddServerFragment;
import nl.eduvpn.app.fragment.ConnectionStatusFragment;
import nl.eduvpn.app.fragment.OrganizationSelectionFragment;
import nl.eduvpn.app.fragment.ServerSelectionFragment;
import nl.eduvpn.app.service.ConnectionService;
import nl.eduvpn.app.service.HistoryService;
import nl.eduvpn.app.service.VPNService;
import nl.eduvpn.app.utils.ErrorDialog;
import nl.eduvpn.app.utils.Log;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    private static final String TAG = MainActivity.class.getName();

    private static final int REQUEST_CODE_SETTINGS = 1001;
    private static final String KEY_BACK_NAVIGATION_ENABLED = "back_navigation_enabled";

    @Inject
    protected HistoryService _historyService;

    @Inject
    protected VPNService _vpnService;

    @Inject
    protected ConnectionService _connectionService;

    private boolean _backNavigationEnabled = false;
    private boolean _parseIntentOnStart = true;

    @Override
    protected int getLayout() {
        return R.layout.activity_main;
    }

    private void createCertExpiryNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.cert_expiry_channel_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            String channelID = Constants.CERT_EXPIRY_NOTIFICATION_CHANNEL_ID;
            NotificationChannel channel = new NotificationChannel(channelID, name, importance);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EduVPNApplication.get(this).component().inject(this);
        setSupportActionBar(binding.toolbar.toolbar);
        _vpnService.onCreate(this);
        if (savedInstanceState == null) {
            // If there's an ongoing VPN connection, open the status screen.
            if (_vpnService.getStatus() != VPNService.VPNStatus.DISCONNECTED) {
                openFragment(new ConnectionStatusFragment(), false);
            } else if (!_historyService.getSavedAuthStateList().isEmpty()) {
                openFragment(ServerSelectionFragment.Companion.newInstance(false), false);
            } else if (BuildConfig.API_DISCOVERY_ENABLED){
                openFragment(new OrganizationSelectionFragment(), false);
            } else {
                openFragment(new AddServerFragment(), false);
            }

        } else if (savedInstanceState.containsKey(KEY_BACK_NAVIGATION_ENABLED)) {
            _backNavigationEnabled = savedInstanceState.getBoolean(KEY_BACK_NAVIGATION_ENABLED);
        }

        _parseIntentOnStart = true;
        binding.toolbar.settingsButton.setOnClickListener(v ->

                onSettingsButtonClicked());
        binding.toolbar.helpButton.setOnClickListener(v ->

                startActivity(new Intent(Intent.ACTION_VIEW, Constants.HELP_URI)));

        createCertExpiryNotificationChannel();
    }

    @Override
    protected void onStart() {
        _connectionService.onStart(this);
        super.onStart();
        if (_parseIntentOnStart) {
            // The app might have been reopened from a URL.
            _parseIntentOnStart = false;
            onNewIntent(getIntent());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        _connectionService.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_BACK_NAVIGATION_ENABLED, _backNavigationEnabled);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AuthorizationResponse authorizationResponse = AuthorizationResponse.fromIntent(intent);
        //noinspection ThrowableResultOfMethodCallIgnored
        AuthorizationException authorizationException = AuthorizationException.fromIntent(intent);
        if (authorizationResponse == null && authorizationException == null) {
            // Not a callback intent.
            return;
        } else {
            // Although this is sensitive info, we only log in this in debug builds.
            Log.i(TAG, "Activity opened with URL: " + intent.getData());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                if (getReferrer() != null) {
                    Log.i(TAG, "Opened from: " + getReferrer().toString());
                }
            }
        }
        if (authorizationException != null) {
            ErrorDialog.show(this, R.string.authorization_error_title, getString(R.string.authorization_error_message,
                    authorizationException.error,
                    authorizationException.code,
                    authorizationException.getMessage()));
        } else {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            ConnectionService.AuthorizationStateCallback callback = () -> {
                if (currentFragment instanceof ServerSelectionFragment) {
                    ((ServerSelectionFragment)currentFragment).connectToSelectedInstance();
                } else {
                    Toast.makeText(this, R.string.provider_added_new_configs_available, Toast.LENGTH_LONG).show();
                }
            };
            _connectionService.parseAuthorizationResponse(authorizationResponse, this, callback);

            // Remove it so we don't parse it again.
            intent.setData(null);

            if (!(currentFragment instanceof ConnectionStatusFragment) && !(currentFragment instanceof ServerSelectionFragment)) {
                openFragment(ServerSelectionFragment.Companion.newInstance(true), false);
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        _vpnService.onDestroy(this);
    }

    public void openFragment(Fragment fragment, boolean openOnTop) {
        if (openOnTop) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .addToBackStack(null)
                    .add(R.id.content_frame, fragment)
                    .commitAllowingStateLoss();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.content_frame, fragment)
                    .commitAllowingStateLoss();
            // Clean the back stack
            for (int i = 0; i < getSupportFragmentManager().getBackStackEntryCount(); i++) {
                if (!getSupportFragmentManager().isStateSaved() && !isFinishing()) {
                    getSupportFragmentManager().popBackStack();
                }
            }
        }
    }

    protected void onSettingsButtonClicked() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SETTINGS);
    }

    public void setBackNavigationEnabled(boolean enabled) {
        _backNavigationEnabled = enabled;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(enabled);
            actionBar.setHomeButtonEnabled(enabled);
        }
    }

    @Override
    public void onBackPressed() {
        if (_backNavigationEnabled) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if (currentFragment instanceof ConnectionStatusFragment) {
                ((ConnectionStatusFragment)currentFragment).returnToHome();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS) {
            if (resultCode == SettingsActivity.RESULT_APP_DATA_CLEARED) {
                if (_vpnService.getStatus() != VPNService.VPNStatus.DISCONNECTED) {
                    _vpnService.disconnect();
                }
                openFragment(new OrganizationSelectionFragment(), false);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
