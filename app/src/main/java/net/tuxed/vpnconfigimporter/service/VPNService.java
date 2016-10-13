package net.tuxed.vpnconfigimporter.service;

import android.content.Context;

import net.tuxed.vpnconfigimporter.utils.Log;

import java.io.IOException;
import java.io.StringReader;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ProfileManager;

/**
 * Service responsible for managing the VPN profiles and the connection.
 * Created by Daniel Zolnai on 2016-10-13.
 */
public class VPNService {

    private static final String TAG = VPNService.class.getName();

    private Context _context;

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
    public boolean importConfig(String configString, String preferredName) {
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
            return true;
        } catch (IOException | ConfigParser.ConfigParseError e) {
            Log.e(TAG, "Error converting profile!", e);
            return false;
        }
    }

}
