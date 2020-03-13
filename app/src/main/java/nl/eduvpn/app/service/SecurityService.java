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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import nl.eduvpn.app.BuildConfig;
import nl.eduvpn.app.utils.Log;

/**
 * Service which is responsible for all things related to security.
 * <p>
 * Created by Daniel Zolnai on 2017-08-01.
 */
public class SecurityService {
    private static final String TAG = SecurityService.class.getName();

    private static String MINISIGN_PUBLIC_KEY_ID;
    private static byte[] MINISIGN_PUBLIC_KEY_BYTES;

    private static final String MINISIGN_ALGO_DESCRIPTION = "Ed";
    private static final int MINISIGN_RANDOM_BYTES_LENGTH = 8;
    private static final int MINISIGN_ED_SIGNATURE_LENGTH = 64;
    private static final int MINISIGN_ED_PUBLIC_KEY_LENGTH = 32;

    private static final byte[] DEPRECATED_PUBLIC_KEY_BYTES = Base64.decode(BuildConfig.DEPRECATED_SIGNATURE_VALIDATION_PUBLIC_KEY, Base64.DEFAULT);

    // We use the ISO-8859-1 charset for converting between strings and bytes,
    // because there one character is exactly one byte.
    // In UTF-8, a character could be 2 or 3 bytes, in ASCII one character is 7 bits only.
    private static final Charset BYTE_DECODE_CHARSET = StandardCharsets.ISO_8859_1;



    static {
        // Init the library
        NaCl.sodium();
    }

    private final Context _context;


    public SecurityService(Context context) {
        _context = context;
        loadMinisignPublicKey(BuildConfig.MINISIGN_SIGNATURE_VALIDATION_PUBLIC_KEY);
    }

    @VisibleForTesting
    static void loadMinisignPublicKey(String publicKey) {
        byte[] publicKeyRawBytes =  Base64.decode(publicKey, Base64.DEFAULT);
        String publicKeyRawString = new String(publicKeyRawBytes, BYTE_DECODE_CHARSET);
        if (MINISIGN_ALGO_DESCRIPTION.length() + MINISIGN_RANDOM_BYTES_LENGTH + MINISIGN_ED_PUBLIC_KEY_LENGTH != publicKeyRawBytes.length) {
            throw new IllegalArgumentException("Invalid public key: not long enough!");
        }
        if (!MINISIGN_ALGO_DESCRIPTION.equals(publicKeyRawString.substring(0, MINISIGN_ALGO_DESCRIPTION.length()))) {
            throw new IllegalArgumentException("Unsupported algorithm, we only support '" + MINISIGN_ALGO_DESCRIPTION + "'!");
        }
        MINISIGN_PUBLIC_KEY_ID = publicKeyRawString.substring(MINISIGN_ALGO_DESCRIPTION.length(), MINISIGN_ALGO_DESCRIPTION.length() + MINISIGN_RANDOM_BYTES_LENGTH);
        MINISIGN_PUBLIC_KEY_BYTES = publicKeyRawString.substring(MINISIGN_ALGO_DESCRIPTION.length() + MINISIGN_RANDOM_BYTES_LENGTH).getBytes(BYTE_DECODE_CHARSET);
    }

    /**
     * @deprecated This will be removed in favor of the regular preferences. Currently
     * we migrate all data over from these preferences to the regular if we detect any data in this one.
     * This method will be probably removed in 1.4 or 1.5. For more info, see: https://github.com/eduvpn/android/issues/117
     */
    @Deprecated
    public SharedPreferences getSecurePreferences() {
        return new SecurePreferences(_context);
    }

    @CheckResult
    public boolean verifyMinisign(@NonNull String message, @NonNull String signatureBase64) throws IOException, IllegalArgumentException {
        String signatureData = getSecondLine(signatureBase64);
        byte[] signatureBytesWithMetadata = Base64.decode(signatureData, Base64.DEFAULT);
        byte[] messageBytes = message.getBytes(BYTE_DECODE_CHARSET);

        String signatureString = new String(signatureBytesWithMetadata, BYTE_DECODE_CHARSET);

        if (MINISIGN_ALGO_DESCRIPTION.length() + MINISIGN_RANDOM_BYTES_LENGTH + MINISIGN_ED_SIGNATURE_LENGTH != signatureBytesWithMetadata.length) {
            throw new IllegalArgumentException("Invalid signature: not long enough!");
        }
        if (!MINISIGN_ALGO_DESCRIPTION.equals(signatureString.substring(0, MINISIGN_ALGO_DESCRIPTION.length()))) {
            throw new IllegalArgumentException("Unsupported algorithm, we only support '" + MINISIGN_ALGO_DESCRIPTION + "'!");
        }
        if (!MINISIGN_PUBLIC_KEY_ID.equals(signatureString.substring(MINISIGN_ALGO_DESCRIPTION.length(), MINISIGN_ALGO_DESCRIPTION.length() + MINISIGN_RANDOM_BYTES_LENGTH))) {
            throw new IllegalArgumentException("Signature does not match public key!");
        }

        byte[] signatureBytes = signatureString.substring(MINISIGN_ALGO_DESCRIPTION.length() + MINISIGN_RANDOM_BYTES_LENGTH).getBytes(BYTE_DECODE_CHARSET);

        int result = Sodium.crypto_sign_verify_detached(signatureBytes, messageBytes, messageBytes.length, MINISIGN_PUBLIC_KEY_BYTES);
        if (result != 0) {
            Log.e(TAG, "Signature validation failed with result: " + result);
            return false;
        }
        return true;
    }

    @CheckResult
    public boolean verifyDeprecatedSignature(String message, String signatureBase64) {
        byte[] signatureBytes = Base64.decode(signatureBase64, Base64.DEFAULT);
        byte[] messageBytes = message.getBytes(BYTE_DECODE_CHARSET);
        int result = Sodium.crypto_sign_verify_detached(signatureBytes, messageBytes, messageBytes.length, DEPRECATED_PUBLIC_KEY_BYTES);
        if (result != 0) {
            Log.e(TAG, "Signature validation failed with result: " + result);
            return false;
        }
        return true;
    }




    /**
     * Generates a cryptographically secure random string using Java's SecureRandom class.
     *
     * @return A string containing Base64 encoded random bytes.
     */
    public String generateSecureRandomString(@Nullable Integer maxLength) {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[128];
        random.nextBytes(randomBytes);
        // We use Base64 to convert random bytes into a string representation, NOT for encryption
        String base64 = Base64.encodeToString(randomBytes, Base64.DEFAULT);
        base64 = base64.replaceAll("\n ", ""); // Strip all newlines and spaces (note: the first param is Regexp)
        if (maxLength != null && maxLength > 0) {
            base64 = base64.substring(0, Math.min(maxLength, base64.length()));
        }
        return base64;
    }

    @VisibleForTesting
    String getSecondLine(String input) throws IOException {
        String[] splitLines = input.split(System.lineSeparator());
        if (splitLines.length < 2) {
            throw new IOException("Input has less than 2 lines!");
        }
        return splitLines[1];
    }
}
