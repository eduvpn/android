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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A configuration for an instance.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class Instance {

    private final String _baseUri;
    private final String _displayName;
    private final String _logoUri;
    private final boolean _isCustom;
    private final @ConnectionType int _connectionType;

    public Instance(@NonNull String baseUri, @NonNull String displayName, @Nullable String logoUri,
                    @ConnectionType int connectionType, boolean isCustom) {
        _baseUri = baseUri;
        _displayName = displayName;
        _logoUri = logoUri;
        _isCustom = isCustom;
        _connectionType = connectionType;
    }

    @NonNull
    public String getBaseURI() {
        return _baseUri;
    }

    @NonNull
    public String getSanitizedBaseURI() {
        if (_baseUri.endsWith("/")) {
            return _baseUri.substring(0, _baseUri.length() - 1);
        }
        return _baseUri;
    }

    @NonNull
    public String getDisplayName() {
        return _displayName;
    }

    @Nullable
    public String getLogoUri() {
        return _logoUri;
    }

    @ConnectionType
    public int getConnectionType() {
        return _connectionType;
    }

    public boolean isCustom() {
        return _isCustom;
    }
}
