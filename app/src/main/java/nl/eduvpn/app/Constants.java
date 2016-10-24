package nl.eduvpn.app;

/**
 * Contains application-wide constant values.
 * Created by Daniel Zolnai on 2016-09-14.
 */
public class Constants {
    public static final boolean DEBUG = BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug");

    public static final String API_DISCOVERY_POSTFIX = "/info.json";
}
