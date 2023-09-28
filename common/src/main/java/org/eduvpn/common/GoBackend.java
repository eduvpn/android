package org.eduvpn.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

public class GoBackend {

    public interface Callback {
        boolean onNewState(int newState, @Nullable String data);
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

    public native DataErrorTuple getAddedServers();

    public native DataErrorTuple getProfiles(int serverType, @NonNull String id, boolean preferTcp, boolean isStartUp);
    public native @Nullable String addServer(int serverType, @NonNull String id);

    public native @Nullable String removeServer(int serverType, @NonNull String id);

    public native @Nullable String handleRedirection(int cookie, @NotNull String url);

}