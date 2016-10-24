package nl.eduvpn.app.entity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A configuration for an instance.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class Instance {

    private String _baseUri;
    private String _displayName;
    private String _logoUri;

    public Instance(@NonNull String baseUri, @NonNull String displayName, @Nullable String logoUri) {
        _baseUri = baseUri;
        _displayName = displayName;
        _logoUri = logoUri;
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
}
