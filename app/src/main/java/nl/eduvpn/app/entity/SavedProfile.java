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
 * Stores the data of a saved profile.
 * Created by Daniel Zolnai on 2016-10-20.
 */
public class SavedProfile {

    private Instance _instance;
    private Profile _profile;
    private String _profileUUID;

    public SavedProfile(Instance instance, Profile profile, String profileUUID) {
        _instance = instance;
        _profile = profile;
        _profileUUID = profileUUID;
    }

    public Instance getInstance() {
        return _instance;
    }

    public Profile getProfile() {
        return _profile;
    }

    public String getProfileUUID() {
        return _profileUUID;
    }
}
