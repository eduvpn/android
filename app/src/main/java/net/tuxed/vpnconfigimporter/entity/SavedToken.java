package net.tuxed.vpnconfigimporter.entity;

/**
 * Stores the mapping between the base URI and the access token.
 * Created by Daniel Zolnai on 2016-10-20.
 */
public class SavedToken {
    private String _baseUri;
    private String _accessToken;

    /**
     * Constructor.
     *
     * @param normalizedBaseUri The normalized base URI of the provider.
     * @param accessToken       The access token which we can use to fetch data.
     */
    public SavedToken(String normalizedBaseUri, String accessToken) {
        _baseUri = normalizedBaseUri;
        _accessToken = accessToken;
    }

    public String getBaseUri() {
        return _baseUri;
    }

    public String getAccessToken() {
        return _accessToken;
    }
}
