package org.eduvpn.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

public class GoBackend {

    public interface Callback {
        boolean onNewState(int newState, @Nullable String data);
        @Nullable String getToken(@NonNull String serverId);
        void setToken(@NonNull String serverId, @Nullable String token);
        void onProxyFileDescriptor(int fileDescriptor);
        void onProxyGuardReady();
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
    public native @Nullable String cookieReply(int cookie, @NotNull String data);
    public native @Nullable String selectProfile(int cookie, @NotNull String profileId);
    public native @Nullable String selectCountry(@NotNull String organizationId, @NotNull String countryCode);
    public native @Nullable String switchProfile(@NotNull String profileId);
    public native DataErrorTuple getCurrentServer();
    public native @Nullable String cancelCookie(int cookie);
    public native @Nullable String deregister();
    public native DataErrorTuple getCertExpiryTimes();
    public native void updateRxBytesRead(long rxBytesRead);
    public native FailoverResult startFailOver(@NotNull String gatewayIp, int mtu);
    public native void notifyConnecting();
    public native void notifyConnected();
    public native void notifyDisconnecting();
    public native void notifyDisconnected();
    public native @Nullable String startProxyGuard(int sourcePort, @NotNull String listen, @NotNull String peer);
}