/*
 *  This file is part of eduVPN.
 *
 *     eduVPN is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     eduVPN is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.eduvpn.app.entity;

/**
 * Represents a VPN connection profile.
 * Created by Daniel Zolnai on 2016-10-11.
 */
public class Profile {

    private String _displayName;
    private String _profileId;

    public Profile(String displayName, String profileId) {
        _displayName = displayName;
        _profileId = profileId;
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
     * Returns the pool ID of this VPN profile.
     */
    public String getProfileId() {
        return _profileId;
    }
}
