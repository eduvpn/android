package org.eduvpn.common;

import androidx.annotation.Nullable;

public class GoBackend {

    static {
        System.loadLibrary("eduvpn_common-" + BuildConfig.COMMON_VERSION);
        System.loadLibrary("eduvpn_common-wrapper");
    }
    public native @Nullable String register(
            String name,
            String version,
            @Nullable String configDirectory,
            int debug
    );
}