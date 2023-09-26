package org.eduvpn.common;

public class GoBackend {

    static {
        System.loadLibrary("eduvpn_common-" + BuildConfig.COMMON_VERSION);
        System.loadLibrary("eduvpn_common-wrapper");
    }

    public native String register(
            String name,
            String version,
            String configDirectory,
            StateCB cb,
            int debug
    );
}