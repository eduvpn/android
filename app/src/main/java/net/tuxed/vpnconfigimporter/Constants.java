package net.tuxed.vpnconfigimporter;

/**
 * Contains application-wide constant values.
 * Created by Daniel Zolnai on 2016-09-14.
 */
public class Constants {
    public static final boolean DEBUG = BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug");
    public static final String APPLICATION_PREFERENCES = "eduvpn_preferences";
    public static final String KEY_STATE = "state";
    public static final String KEY_HOST = "host";
    public static final String KEY_INTENT_ACTION_STATUS = "de.blinkt.openvpn.VPN_STATUS";
    public static final String KEY_EXTRA_STATUS = "status";
}
