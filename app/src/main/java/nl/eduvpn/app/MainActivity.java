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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;
import nl.eduvpn.app.base.BaseActivity;
import nl.eduvpn.app.databinding.ActivityMainBinding;
import nl.eduvpn.app.fragment.ConnectionStatusFragment;
import nl.eduvpn.app.fragment.CustomProviderFragment;
import nl.eduvpn.app.fragment.HomeFragment;
import nl.eduvpn.app.fragment.TypeSelectorFragment;
import nl.eduvpn.app.service.ConnectionService;
import nl.eduvpn.app.service.HistoryService;
import nl.eduvpn.app.service.VPNService;
import nl.eduvpn.app.utils.ErrorDialog;
import nl.eduvpn.app.utils.Log;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    private static final String TAG = MainActivity.class.getName();

    @Inject
    protected HistoryService _historyService;

    @Inject
    protected VPNService _vpnService;

    @Inject
    protected ConnectionService _connectionService;

    @Override
    protected int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EduVPNApplication.get(this).component().inject(this);
        setSupportActionBar(binding.toolbar);
        _vpnService.onCreate(this);
        if (savedInstanceState == null) {
            // If there's an ongoing VPN connection, open the status screen.
            if (_vpnService.getStatus() != VPNService.VPNStatus.DISCONNECTED) {
                openFragment(new ConnectionStatusFragment(), false);
            } else if (!_historyService.getSavedAuthStateList().isEmpty()) {
                openFragment(new HomeFragment(), false);
            } else {
                // User has no previously saved profiles. Show the type selector.
                if (BuildConfig.API_DISCOVERY_ENABLED) {
                    // eduVPN flavor
                    openFragment(new TypeSelectorFragment(), false);
                } else {
                    // Let's Connect! flavor
                    openFragment(new CustomProviderFragment(), false);
                }
            }
        } // else the activity will automatically restore everything.
        // The app might have been reopened from a URL.
        onNewIntent(getIntent());
        binding.settingsButton.setOnClickListener(v -> onSettingsButtonClicked());
    }

    @Override
    protected void onStart() {
        _connectionService.onStart(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        _connectionService.onStop();
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
        if (_vpnService.getStatus() != VPNService.VPNStatus.DISCONNECTED) {
            // The user clicked on an authorization link while the VPN is connected.
            // Maybe just a mistake?
            Toast.makeText(this, R.string.already_connected_please_disconnect, Toast.LENGTH_LONG).show();
            return;
        }
        if (authorizationException != null) {
            ErrorDialog.show(this, R.string.authorization_error_title, getString(R.string.authorization_error_message,
                    authorizationException.error,
                    authorizationException.code,
                    authorizationException.getMessage()));
        } else {
            _connectionService.parseAuthorizationResponse(authorizationResponse, this);
            // Remove it so we don't parse it again.
            intent.setData(null);
            // Show the home fragment, so the user can select his new config(s)
            HomeFragment fragment = HomeFragment.newInstance(true);
            openFragment(fragment, false);
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
                    .addToBackStack(null)
                    .add(R.id.contentFrame, fragment)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contentFrame, fragment)
                    .commitAllowingStateLoss();
        }
    }

    protected void onSettingsButtonClicked() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
