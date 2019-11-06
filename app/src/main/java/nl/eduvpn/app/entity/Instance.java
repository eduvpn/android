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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A configuration for an instance.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class Instance {

    private String _baseUri;
    private String _displayName;
    private String _logoUri;
    private boolean _isCustom;
    private AuthorizationType _authorizationType;

    public Instance(@NonNull String baseUri, @NonNull String displayName, @Nullable String logoUri,
                    AuthorizationType authorizationType, boolean isCustom) {
        _baseUri = baseUri;
        _displayName = displayName;
        _logoUri = logoUri;
        _isCustom = isCustom;
        _authorizationType = authorizationType;
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

    public AuthorizationType getAuthorizationType() {
        return _authorizationType;
    }

    public boolean isCustom() {
        return _isCustom;
    }

    public void setAuthorizationType(AuthorizationType authorizationType) {
        _authorizationType = authorizationType;
    }
}
