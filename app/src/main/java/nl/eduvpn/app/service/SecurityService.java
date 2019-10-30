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

package nl.eduvpn.app.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.securepreferences.SecurePreferences;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

import java.nio.charset.Charset;
import java.security.SecureRandom;

import androidx.annotation.Nullable;
import nl.eduvpn.app.BuildConfig;
import nl.eduvpn.app.utils.Log;

/**
 * Service which is responsible for all things related to security.
 * <p>
 * Created by Daniel Zolnai on 2017-08-01.
 */
public class SecurityService {
    private static final String TAG = SecurityService.class.getName();

    private static final byte[] PUBLIC_KEY_BYTES = Base64.decode(BuildConfig.SIGNATURE_VALIDATION_PUBLIC_KEY, Base64.DEFAULT);

    static {
        // Init the library
        NaCl.sodium();
    }

    private final Context _context;

    public SecurityService(Context context) {
        _context = context;
    }

    public SharedPreferences getSecurePreferences() {
        return new SecurePreferences(_context);
    }

    public boolean isValidSignature(String message, String signatureBase64) {
        byte[] signatureBytes = Base64.decode(signatureBase64, Base64.DEFAULT);
        byte[] messageBytes = message.getBytes(Charset.forName("UTF-8"));
        int result = Sodium.crypto_sign_verify_detached(signatureBytes, messageBytes, messageBytes.length, PUBLIC_KEY_BYTES);
        if (result != 0) {
            Log.e(TAG, "Signature validation failed with result: " + result);
            return false;
        }
        return true;
    }

    /**
     * Generates a cryptograhically secure random string using Java's SecureRandom class.
     *
     * @return A string containing Base64 encoded random bytes.
     */
    public String generateSecureRandomString(@Nullable Integer maxLength) {
        SecureRandom random = new SecureRandom();
        byte randomBytes[] = new byte[128];
        random.nextBytes(randomBytes);
        // We use Base64 to convert random bytes into a string representation, NOT for encryption
        String base64 = Base64.encodeToString(randomBytes, Base64.DEFAULT);
        base64 = base64.replaceAll("\n ", ""); // Strip all newlines and spaces (note: the first param is Regexp)
        if (maxLength != null && maxLength > 0) {
            base64 = base64.substring(0, Math.min(maxLength, base64.length()));
        }
        return base64;
    }
}
