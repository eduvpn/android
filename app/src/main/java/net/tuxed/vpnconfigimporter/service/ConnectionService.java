package net.tuxed.vpnconfigimporter.service;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.utils.Log;

import java.util.UUID;

/**
 * The connection service takes care of building up the URLs and validating the result.
 * Created by Daniel Zolnai on 2016-10-11.
 */
public class ConnectionService {

    public class InvalidConnectionAttemptException extends Exception {
        public InvalidConnectionAttemptException(String message) {
            super(message);
        }
    }

    private static final String TAG = ConnectionService.class.getName();

    private static final String SCOPE = "config";
    private static final String RESPONSE_TYPE = "token";
    private static final String REDIRECT_URI = "vpn://import/callback";
    private static final String CLIENT_ID = "vpn-companion";

    private PreferencesService _preferencesService;
    private Context _context;
    private String _accessToken;

    /**
     * Constructor.
     *
     * @param context            The application or activity context.
     * @param preferencesService The preferences service used to store temporary data.
     */
    public ConnectionService(Context context, PreferencesService preferencesService) {
        _context = context;
        _preferencesService = preferencesService;
        _accessToken = preferencesService.getAccessToken();
    }

    /**
     * Builds up the connection URL from the base URL and the random state.
     *
     * @param baseUrl The base URL of the VPN provider.
     * @param state   The random state.
     * @return The connection URL which should be opened in the browser.
     */
    private String _buildConnectionUrl(String baseUrl, String state) {
        return baseUrl + "/portal/_oauth/authorize?client_id=" + CLIENT_ID +
                "&redirect_uri=" + REDIRECT_URI +
                "&response_type=" + RESPONSE_TYPE +
                "&scope=" + SCOPE +
                "&state=" + state;
    }

    /**
     * Generates a new random state.
     *
     * @return A new random state string.
     */
    private String _generateState() {
        return UUID.randomUUID().toString();
    }

    /**
     * Creates an intent which will open the site where the authorization can be initiated.
     *
     * @param baseUrl The base URL of the VPN provider.
     * @return An intent which can be started.
     */
    public Intent getAuthorizationIntent(String baseUrl) {
        // Remove the '/' character from the end.
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String state = _generateState();
        _preferencesService.saveConnectionBaseUrl(baseUrl);
        _preferencesService.saveConnectionState(state);
        String connectionUrl = _buildConnectionUrl(baseUrl, state);
        // Create an intent which opens the URL in the default browser
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(connectionUrl));
        return intent;
    }

    /**
     * Checks if the returned state is valid.
     *
     * @param state The returned state.
     * @return True if the state is valid and matches the saved state. Else false.
     */
    private boolean _validateState(String state) {
        String savedState = _preferencesService.getConnectionState();
        return state != null && savedState != null && savedState.equals(state);
    }

    /**
     * Parses the callback intent and retrieves the access token.
     *
     * @param intent The intent to parse.
     * @throws InvalidConnectionAttemptException Thrown if there's a problem with the callback.
     */
    public void parseCallbackIntent(Intent intent) throws InvalidConnectionAttemptException {
        Uri callbackUri = intent.getData();
        Log.i(TAG, "Got callback URL: " + callbackUri.toString());

        String fragment = callbackUri.getFragment();
        String[] fragments = fragment.split("&");

        String accessToken = null;
        String state = null;

        for (String element : fragments) {
            String[] keyValuePair = element.split("=");
            if (keyValuePair[0].equals("access_token")) {
                // Found access token
                accessToken = keyValuePair[1];
            }
            if (keyValuePair[0].equals("state")) {
                // Found state
                state = keyValuePair[1];
            }
        }

        if (accessToken == null) {
            throw new InvalidConnectionAttemptException(_context.getString(R.string.error_access_token_missing));
        }
        if (state == null) {
            throw new InvalidConnectionAttemptException(_context.getString(R.string.error_state_missing));
        }
        // Make sure the state is valid
        boolean isStateValid = _validateState(state);

        if (!isStateValid) {
            throw new InvalidConnectionAttemptException(_context.getString(R.string.error_state_mismatch));
        }
        // Save the access token
        _accessToken = accessToken;
        _preferencesService.saveAccessToken(accessToken);
        // Now we can delete the saved state
        _preferencesService.removeSavedConnectionState();
    }

    public String getAccessToken() {
        return _accessToken;
    }
}
