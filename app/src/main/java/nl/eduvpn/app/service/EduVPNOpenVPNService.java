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

import android.app.Activity;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.Connection;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import nl.eduvpn.app.R;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.entity.SavedProfile;
import nl.eduvpn.app.livedata.ByteCount;
import nl.eduvpn.app.livedata.IPs;
import nl.eduvpn.app.livedata.UnlessDisconnectedLiveData;
import nl.eduvpn.app.livedata.openvpn.ByteCountLiveData;
import nl.eduvpn.app.livedata.openvpn.IPLiveData;
import nl.eduvpn.app.utils.Log;

/**
 * Service responsible for managing the OpenVPN profiles and the connection.
 * Created by Daniel Zolnai on 2016-10-13.
 */
public class EduVPNOpenVPNService extends VPNService implements VpnStatus.StateListener {

    private static final String TAG = EduVPNOpenVPNService.class.getName();

    private Context _context;

    private PreferencesService _preferencesService;

    // Stores the current VPN status.
    private ConnectionStatus _connectionStatus = ConnectionStatus.LEVEL_NOTCONNECTED;
    private Handler _updatesHandler = new Handler();

    private Integer _errorResource;

    private IOpenVPNServiceInternal _openVPNService;

    private ServiceConnection _serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            _openVPNService = IOpenVPNServiceInternal.Stub.asInterface(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            _openVPNService = null;
        }
    };

    private IPLiveData _ipLiveData;
    private LiveData<ByteCount> _byteCountLiveData;

    /**
     * Constructor.
     *
     * @param context The application or activity context.
     */
    public EduVPNOpenVPNService(Context context, PreferencesService preferencesService, IPLiveData ipLiveData) {
        _context = context;
        _preferencesService = preferencesService;
        _ipLiveData = ipLiveData;
        _byteCountLiveData = UnlessDisconnectedLiveData.INSTANCE.create(new ByteCountLiveData(), this);
    }

    /**
     * Call this when your activity is starting up.
     *
     * @param activity The current activity to bind the service with.
     */
    public void onCreate(@NonNull Activity activity) {
        OpenVPNService.setNotificationActivityClass(activity.getClass());
        Intent intent = new Intent(activity, OpenVPNService.class);
        intent.putExtra(OpenVPNService.ALWAYS_SHOW_NOTIFICATION, false);
        intent.setAction(OpenVPNService.START_SERVICE);
        activity.bindService(intent, _serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onActive() {
        VpnStatus.addStateListener(this);
    }

    @Override
    public void onInactive() {
        VpnStatus.removeStateListener(this);
    }

    /**
     * Call this when the activity is being destroyed.
     * This does not shut down the VPN connection, only removes the listeners from it. The listeners will be reattached
     * on the next startup.
     *
     * @param activity The activity being destroyed.
     */
    public void onDestroy(@NonNull Activity activity) {
        activity.unbindService(_serviceConnection);
    }

    @NonNull
    @Override
    public LiveData<ByteCount> getByteCountLiveData() {
        return _byteCountLiveData;
    }

    @NonNull
    @Override
    public LiveData<IPs> getIpLiveData() {
        return _ipLiveData;
    }

    /**
     * Imports a config which is represented by a string.
     *
     * @param configString  The config as a string.
     * @param preferredName The preferred name for the config.
     * @return True if the import was successful, false if it failed.
     */
    @Nullable
    public VpnProfile importConfig(String configString, String preferredName, @Nullable SavedKeyPair savedKeyPair) {
        if (savedKeyPair != null) {
            Log.d(TAG, "Adding info from saved key pair to the config...");
            configString = configString + "\n<cert>\n" + savedKeyPair.getKeyPair().getCertificate() + "\n</cert>\n" +
                    "\n<key>\n" + savedKeyPair.getKeyPair().getPrivateKey() + "\n</key>\n";
        }
        ConfigParser configParser = new ConfigParser();
        try {
            configParser.parseConfig(new StringReader(configString));
            VpnProfile profile = configParser.convertProfile();
            profile.mAlias = _context.getString(R.string.app_name);
            if (preferredName != null) {
                profile.mName = preferredName;
            }
            ProfileManager profileManager = ProfileManager.getInstance(_context);
            // We only have one profile at a time saved.
            List<VpnProfile> profiles = new ArrayList<>(profileManager.getProfiles());
            for (VpnProfile savedProfile : profiles) {
                profileManager.removeProfile(_context, savedProfile);
            }
            profileManager.addProfile(profile);
            profileManager.saveProfile(_context, profile);
            profileManager.saveProfileList(_context);
            Log.i(TAG, "Added and saved profile with UUID: " + profile.getUUIDString());
            return profile;
        } catch (IOException | ConfigParser.ConfigParseError e) {
            Log.e(TAG, "Error converting profile!", e);
            return null;
        }
    }

    /***
     * Finds a matching VPN library profile for a profile saved by the application.
     * It should only exist if this is the most recently connected profile.
     *
     * @param savedProfile The profile saved by this application in its preferences.
     * @return The VPN library profile, if found. Otherwise null.
     */
    @Nullable
    public VpnProfile findMatchingVpnProfile(SavedProfile savedProfile) {
        ProfileManager profileManager = ProfileManager.getInstance(_context);
        for (VpnProfile profile : profileManager.getProfiles()) {
            if (profile.getUUIDString().equals(savedProfile.getProfileUUID())) {
                return profile;
            }
        }
        return null;
    }

    /**
     * Connects to the VPN using the profile supplied as a parameter.
     *
     * @param activity   The current activity, required for providing a context.
     * @param vpnProfile The profile to connect to.
     */
    public void connect(@NonNull Activity activity, @NonNull VpnProfile vpnProfile) {
        Log.i(TAG, "Initiating connection with profile:" + vpnProfile.getUUIDString());
        boolean forceTcp = _preferencesService.getAppSettings().forceTcp();
        Log.i(TAG, "Force TCP: " + forceTcp);
        // If force TCP is enabled, disable the UDP connections
        for (Connection connection : vpnProfile.mConnections) {
            if (connection.mUseUdp) {
                connection.mEnabled = !forceTcp;
            }
        }
        // Make sure these changes are NOT saved, since we don't want the config changes to be permanent.
        Intent intent = new Intent(activity, LaunchVPN.class);
        intent.putExtra(LaunchVPN.EXTRA_KEY, vpnProfile.getUUIDString());
        intent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);
        intent.setAction(Intent.ACTION_MAIN);
        activity.startActivity(intent);
    }

    @Override
    public void startForeground(int id, @NonNull Notification notification) {
        // We do not use the notification provided by the OpenVPN library because it is not possible
        // to execute an action when the user presses disconnect in the notification. When the user
        // presses disconnect, we need to send a /disconnect call to the API. We could have added
        // this functionality to the OpenVPN library, but we have to use our own notification for
        // WireGuard anyway because the WireGuard library does not provide a notification, so we
        // might as well use the same notification for all VPN implementations.
        try {
            _openVPNService.startForeground(id, notification);
        } catch (RemoteException ex) {
            Log.e(TAG, "Exception when trying to start foreground service.", ex);
        }
    }

    /**
     * Disconnects the current VPN connection.
     */
    public void disconnect() {
        try {
            _openVPNService.stopVPN(false);
        } catch (RemoteException ex) {
            Log.e(TAG, "Exception when trying to stop connection. Connection might not be closed!", ex);
        }
        _onDisconnect();
    }

    /**
     * Call this if the service has disconnected. Resets all statistics.
     */
    private void _onDisconnect() {
        // Reset all statistics
        _errorResource = null;
    }

    /**
     * Converts a connection level to a more simple status.
     */
    public static VPNStatus connectionStatusToVPNStatus(ConnectionStatus connectionStatus) {
        switch (connectionStatus) {
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_CONNECTING_SERVER_REPLIED:
            case LEVEL_START:
                return VPNStatus.CONNECTING;
            case LEVEL_CONNECTED:
                return VPNStatus.CONNECTED;
            case LEVEL_VPNPAUSED:
                return VPNStatus.PAUSED;
            case LEVEL_AUTH_FAILED:
                return VPNStatus.FAILED;
            case LEVEL_NONETWORK:
            case LEVEL_NOTCONNECTED:
            case LEVEL_WAITING_FOR_USER_INPUT:
            case UNKNOWN_LEVEL:
                return VPNStatus.DISCONNECTED;
            default:
                throw new RuntimeException("Unhandled VPN connection level!");
        }
    }

    /**
     * Returns a more simple status for the current connection level.
     *
     * @return The current status of the VPN.
     */
    @NonNull
    public VPNStatus getStatus() {
        return connectionStatusToVPNStatus(_connectionStatus);
    }

    /**
     * Returns the error string.
     *
     * @return The description of the error.
     */
    public String getErrorString() {
        if (_errorResource != null) {
            return _context.getString(_errorResource);
        } else {
            return null;
        }
    }

    @Override
    public void setConnectedVPN(String uuid) {
        Log.i(TAG, "Connected with profile: " + uuid);
    }


    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level, Intent Intent) {
        VPNService.VPNStatus oldStatus = connectionStatusToVPNStatus(_connectionStatus);
        _connectionStatus = level;
        VPNService.VPNStatus status = connectionStatusToVPNStatus(level);
        if (status == oldStatus) {
            // Nothing changed.
            return;
        }
        if (status == VPNStatus.FAILED) {
            _errorResource = localizedResId;
        } else if (status == VPNStatus.DISCONNECTED) {
            _onDisconnect();
        }
        // Notify the observers.
        _updatesHandler.post(() -> {
            setValue(status);
        });
    }

    /**
     * Returns the profile with the given UUID.
     *
     * @param profileUUID The UUID of the profile.
     * @return The profile if found, otherwise null.
     */
    @Nullable
    public VpnProfile getProfileWithUUID(@NonNull String profileUUID) {
        ProfileManager profileManager = ProfileManager.getInstance(_context);
        for (VpnProfile vpnProfile : profileManager.getProfiles()) {
            if (vpnProfile.getUUIDString().equals(profileUUID)) {
                return vpnProfile;
            }
        }
        return null;
    }

    @NotNull
    public String getProtocolName() {
        return "OpenVPN";
    }
}
