package net.tuxed.vpnconfigimporter.entity;

import java.io.StringReader;

/**
 * Represents a VPN connection profile.
 * Created by Daniel Zolnai on 2016-10-11.
 */
public class Profile {

    private String _displayName;
    private String _poolId;
    private Boolean _twoFactor;

    public Profile(String displayName, String poolId, Boolean twoFactor) {
        _displayName = displayName;
        _poolId = poolId;
        _twoFactor = twoFactor;
    }

    /**
     * Returns the display name of this profile.
     *
     * @return How this profile should be mentioned.
     */
    public String getDisplayName() {
        return _displayName;
    }

    /**
     * Returns if this profile supports two-factor authentication.
     *
     * @return If the profile supports two-factor authentication.
     */
    public Boolean getTwoFactor() {
        return _twoFactor;
    }

    /**
     * Returns the pool ID of this VPN profile.
     */
    public String getPoolId() {
        return _poolId;
    }
}
