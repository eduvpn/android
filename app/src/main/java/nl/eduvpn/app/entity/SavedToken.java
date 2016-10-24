package net.tuxed.vpnconfigimporter.entity;

/**
 * Stores the mapping between the base URI and the access token.
 * Created by Daniel Zolnai on 2016-10-20.
 */
public class SavedToken {
    private Instance _instance;
    private String _accessToken;

    /**
     * Constructor.
     *
     * @param instance    The VPN provider the token is valid for.
     * @param accessToken The access token which we can use to fetch data.
     */
    public SavedToken(Instance instance, String accessToken) {
        _instance = instance;
        _accessToken = accessToken;
    }

    public Instance getInstance() {
        return _instance;
    }

    public String getAccessToken() {
        return _accessToken;
    }
}
