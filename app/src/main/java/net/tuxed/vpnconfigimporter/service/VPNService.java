package net.tuxed.vpnconfigimporter.service;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.utils.Log;

import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Observable;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Service responsible for managing the VPN profiles and the connection.
 * Created by Daniel Zolnai on 2016-10-13.
 */
public class VPNService extends Observable implements VpnStatus.StateListener {

    public enum VPNStatus {
        DISCONNECTED, CONNECTING, CONNECTED, PAUSED
    }

    private static final Long CONNECTION_INFO_UPDATE_INTERVAL_MS = 1000L;

    private static final String TAG = VPNService.class.getName();

    private Context _context;

    // Stores the current VPN status.
    private VpnStatus.ConnectionStatus _connectionStatus = VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED;
    // These are used to provide connection info updates
    private ConnectionInfoCallback _connectionInfoCallback;
    private Handler _updatesHandler = new Handler();
    // These store the current connection statistics
    private Date _connectionTime;
    private Long _bytesIn;
    private Long _bytesOut;
    private String _serverIpV4;
    private String _serverIpV6;

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

    private VpnStatus.ByteCountListener _byteCountListener = new VpnStatus.ByteCountListener() {
        @Override
        public void updateByteCount(long in, long out, long diffIn, long diffOut) {
            _bytesIn = in;
            _bytesOut = out;
        }
    };


    public void onCreate(Activity activity) {
        VpnStatus.addStateListener(this);
        Intent intent = new Intent(activity, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        activity.bindService(intent, _serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void onDestroy(Activity activity) {
        activity.unbindService(_serviceConnection);
        VpnStatus.removeStateListener(this);
    }

    /**
     * Constructor.
     *
     * @param context The application or activity context.
     */
    public VPNService(Context context) {
        _context = context;
    }

    /**
     * Imports a config which is represented by a string.
     *
     * @param configString  The config as a string.
     * @param preferredName The preferred name for the config.
     * @return True if the import was successful, false if it failed.
     */
    public VpnProfile importConfig(String configString, String preferredName) {
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
    public void connect(Activity activity, VpnProfile vpnProfile) {
        Log.i(TAG, String.format("Initiating connection with profile '%s'", vpnProfile.getUUIDString()));
        Intent intent = new Intent(activity, LaunchVPN.class);
        intent.putExtra(LaunchVPN.EXTRA_KEY, vpnProfile.getUUIDString());
        intent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);
        intent.setAction(Intent.ACTION_MAIN);
        activity.startActivity(intent);
    }

    public void disconnect() {
        _openVPNService.getManagement().stopVPN(false);
        // Reset all statistics
        detachConnectionInfoListener();
        _updatesHandler.removeCallbacksAndMessages(null);
        _connectionTime = null;
        _bytesIn = null;
        _bytesOut = null;
        _serverIpV4 = null;
        _serverIpV6 = null;
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
            case LEVEL_NONETWORK:
            case LEVEL_NOTCONNECTED:
            case LEVEL_WAITING_FOR_USER_INPUT:
            case UNKNOWN_LEVEL:
                return VPNStatus.DISCONNECTED;
            default:
                throw new RuntimeException("Unhandled VPN connection level!");
        }
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, VpnStatus.ConnectionStatus level) {
        VpnStatus.ConnectionStatus oldStatus = _connectionStatus;
        _connectionStatus = level;
        if (_connectionStatus == oldStatus) {
            // Nothing changed.
            return;
        }
        if (getStatus() == VPNStatus.CONNECTED) {
            _connectionTime = new Date();
            // Set the other variables for the metadata
            _serverIpV4 = _openVPNService.getLastLocalIpV4Address();
            _serverIpV6 = _openVPNService.getLastLocalIpV6Address();
            if (_connectionInfoCallback != null) {
                _updatesHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        _connectionInfoCallback.metadataAvailable(_serverIpV4, _serverIpV6);
                    }
                });
            }
        }
        setChanged();
        notifyObservers(getStatus());
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
        VpnStatus.addByteCountListener(_byteCountListener);
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
     * Detaches the current connection info listener.
     */
    public void detachConnectionInfoListener() {
        _connectionInfoCallback = null;
        VpnStatus.removeByteCountListener(_byteCountListener);
    }

    public interface ConnectionInfoCallback {
        void updateStatus(Long secondsConnected, Long bytesIn, Long bytesOut);

        void metadataAvailable(String localIpV4, String localIpV6);
    }


}
