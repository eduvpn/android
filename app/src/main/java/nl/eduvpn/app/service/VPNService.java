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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.Connection;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import nl.eduvpn.app.entity.SavedKeyPair;
import nl.eduvpn.app.utils.Log;

/**
 * Service responsible for managing the VPN profiles and the connection.
 * Created by Daniel Zolnai on 2016-10-13.
 */
public class VPNService extends Observable implements VpnStatus.StateListener {

    public enum VPNStatus {
        DISCONNECTED, CONNECTING, CONNECTED, PAUSED, FAILED
    }

    private static final Long CONNECTION_INFO_UPDATE_INTERVAL_MS = 1000L;
    private static final String VPN_INTERFACE_NAME = "tun0";

    private static final String TAG = VPNService.class.getName();

    private Context _context;

    private PreferencesService _preferencesService;

    // Stores the current VPN status.
    private ConnectionStatus _connectionStatus = ConnectionStatus.LEVEL_NOTCONNECTED;
    // These are used to provide connection info updates
    private ConnectionInfoCallback _connectionInfoCallback;
    private Handler _updatesHandler = new Handler();
    // These store the current connection statistics
    private Date _connectionTime;
    private Long _bytesIn;
    private Long _bytesOut;
    private String _serverIpV4;
    private String _serverIpV6;
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

    private VpnStatus.ByteCountListener _byteCountListener = new VpnStatus.ByteCountListener() {
        @Override
        public void updateByteCount(long in, long out, long diffIn, long diffOut) {
            _bytesIn = in;
            _bytesOut = out;
        }
    };

    /**
     * Constructor.
     *
     * @param context The application or activity context.
     */
    public VPNService(Context context, PreferencesService preferencesService) {
        _context = context;
        _preferencesService = preferencesService;
    }


    /**
     * Call this when your activity is starting up.
     *
     * @param activity The current activity to bind the service with.
     */
    public void onCreate(@NonNull Activity activity) {
        OpenVPNService.setNotificationActivityClass(activity.getClass());
        VpnStatus.addStateListener(this);
        Intent intent = new Intent(activity, OpenVPNService.class);
        intent.putExtra(OpenVPNService.ALWAYS_SHOW_NOTIFICATION, true);
        intent.setAction(OpenVPNService.START_SERVICE);
        activity.bindService(intent, _serviceConnection, Context.BIND_AUTO_CREATE);
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
        VpnStatus.removeStateListener(this);
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
            if (preferredName != null) {
                profile.mName = preferredName;
            }
            ProfileManager profileManager = ProfileManager.getInstance(_context);
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
        _connectionTime = null;
        _bytesIn = null;
        _bytesOut = null;
        _serverIpV4 = null;
        _serverIpV6 = null;
        _errorResource = null;
    }


    /**
     * Returns a more simple status for the current connection level.
     *
     * @return The current status of the VPN.
     */
    public VPNStatus getStatus() {
        switch (_connectionStatus) {
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

    /**
     * Retrieves the IP4 and IPv6 addresses assigned by the VPN server to this client using a network interface lookup.
     *
     * @return The IPv4 and IPv6 addresses in this order as a pair. If not found, a null value is returned instead.
     */
    private Pair<String, String> _lookupVpnIpAddresses() {
        try {
            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : networkInterfaces) {
                if (VPN_INTERFACE_NAME.equals(networkInterface.getName())) {
                    List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                    String ipV4 = null;
                    String ipV6 = null;
                    for (InetAddress address : addresses) {
                        String ip = address.getHostAddress();
                        boolean isIPv4 = ip.indexOf(':') < 0;
                        if (isIPv4) {
                            ipV4 = ip;
                        } else {
                            int delimiter = ip.indexOf('%');
                            ipV6 = delimiter < 0 ? ip.toLowerCase() : ip.substring(0, delimiter).toLowerCase();
                        }
                    }
                    if (ipV4 != null || ipV6 != null) {
                        return new Pair<>(ipV4, ipV6);
                    } else {
                        return null;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.w(TAG, "Unable to retrieve network interface info!", ex);
        }
        return null;
    }

    /**
     * Parses the IPv4 and IPv6 from the log message.
     *
     * @param logMessage The log message to parse from.
     * @return The IPv4 and IPv6 addresses as a pair in this order. If the parsing failed (unexpected format), then a null value will be returned.
     */
    private Pair<String, String> _parseVpnIpAddressesFromLogMessage(String logMessage) {
        if (logMessage != null && logMessage.length() > 0) {
            String[] splits = logMessage.split(Pattern.quote(","));
            if (splits.length == 7) {
                String ipV4 = splits[1];
                String ipV6 = splits[6];
                if (ipV4.length() == 0) {
                    ipV4 = null;
                }
                if (ipV6.length() == 0) {
                    ipV6 = null;
                }
                return new Pair<>(ipV4, ipV6);
            }
        }
        return null;
    }


    @Override
    public void setConnectedVPN(String uuid) {
        Log.i(TAG, "Connected with profile: " + uuid);
    }

    @Override
    public void updateState(String state, String logMessage, int localizedResId, ConnectionStatus level) {
        ConnectionStatus oldStatus = _connectionStatus;
        _connectionStatus = level;
        if (_connectionStatus == oldStatus) {
            // Nothing changed.
            return;
        }
        if (getStatus() == VPNStatus.CONNECTED) {
            VpnStatus.addByteCountListener(_byteCountListener);
            _connectionTime = new Date();
            // Try to get the address from a lookup
            Pair<String, String> ips = _lookupVpnIpAddresses();
            if (ips != null) {
                _serverIpV4 = ips.first;
                _serverIpV6 = ips.second;
            } else {
                Log.i(TAG, "Unable to determine IP addresses from network interface lookup, using log message instead.");
                ips = _parseVpnIpAddressesFromLogMessage(logMessage);
                if (ips != null) {
                    _serverIpV4 = ips.first;
                    _serverIpV6 = ips.second;
                }
            }
            if (_connectionInfoCallback != null) {
                _updatesHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        _connectionInfoCallback.metadataAvailable(_serverIpV4, _serverIpV6);
                    }
                });
            }
        } else if (getStatus() == VPNStatus.FAILED) {
            _errorResource = localizedResId;
        } else if (getStatus() == VPNStatus.DISCONNECTED) {
            _onDisconnect();
        }
        // Notify the observers.
        _updatesHandler.post(() -> {
            setChanged();
            notifyObservers(getStatus());
        });
    }


    /**
     * Attaches a connection info listener callback, which will be called frequently with the latest data.
     *
     * @param callback The callback.
     */
    public void attachConnectionInfoListener(ConnectionInfoCallback callback) {
        _connectionInfoCallback = callback;
        if (_serverIpV4 != null && _serverIpV6 != null) {
            _connectionInfoCallback.metadataAvailable(_serverIpV4, _serverIpV6);
        }
        _updatesHandler.post(new Runnable() {
            @Override
            public void run() {
                if (_connectionInfoCallback != null) {
                    Long secondsElapsed = null;
                    if (_connectionTime != null) {
                        secondsElapsed = (Calendar.getInstance().getTimeInMillis() - _connectionTime.getTime()) / 1000L;
                    }
                    _connectionInfoCallback.updateStatus(secondsElapsed, _bytesIn, _bytesOut);
                    _updatesHandler.postDelayed(this, CONNECTION_INFO_UPDATE_INTERVAL_MS);
                }
            }
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

    /**
     * Detaches the current connection info listener.
     */
    public void detachConnectionInfoListener() {
        _connectionInfoCallback = null;
        _updatesHandler.removeCallbacksAndMessages(null);
        VpnStatus.removeByteCountListener(_byteCountListener);
    }

    public interface ConnectionInfoCallback {
        void updateStatus(Long secondsConnected, Long bytesIn, Long bytesOut);

        void metadataAvailable(String localIpV4, String localIpV6);
    }


}
