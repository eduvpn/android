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
 * Contains the application settings.
 * Created by Daniel Zolnai on 2016-10-22.
 */
public class Settings {

    public static final boolean USE_CUSTOM_TABS_DEFAULT_VALUE = true;
    public static final boolean FORCE_TCP_DEFAULT_VALUE = false;
    public static final boolean USE_WIREGUARD_DEFAULT_VALUE = false;

    private boolean _useCustomTabs;
    private boolean _forceTcp;
    private boolean _useWireGuard;

    public Settings(boolean useCustomTabs, boolean forceTcp, boolean useWireGuard) {
        _useCustomTabs = useCustomTabs;
        _forceTcp = forceTcp;
        _useWireGuard = useWireGuard;
    }

    public boolean useCustomTabs() {
        return _useCustomTabs;
    }

    public boolean forceTcp() {
        return _forceTcp;
    }

    public boolean useWireGuard() {
        return _useWireGuard;
    }
}
