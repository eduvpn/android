package org.eduvpn.common;

import androidx.annotation.Nullable;

public class GoBackend {

    public interface Callback {
        void onNewState(int newState, @Nullable String data);
    }

    public static Callback callbackFunction = null;

    static {
        System.loadLibrary("eduvpn_common-" + BuildConfig.COMMON_VERSION);
        System.loadLibrary("eduvpn_common-wrapper");
    }

    public native @Nullable String register(
            String name,
            String version,
            @Nullable String configDirectory,
            boolean debug
    );

    public native DataErrorTuple discoverOrganizations();

    public native DataErrorTuple discoverServers();

    public native @Nullable String addServer(int serverType, String id);
}