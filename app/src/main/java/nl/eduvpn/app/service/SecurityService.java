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

import android.util.Base64;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.eduvpn.app.BuildConfig;
import nl.eduvpn.app.utils.Log;

/**
 * Service which is responsible for all things related to security.
 * <p>
 * Created by Daniel Zolnai on 2017-08-01.
 */
public class SecurityService {
    private static final String TAG = SecurityService.class.getName();

    private static List<String> MINISIGN_PUBLIC_KEY_ID_LIST;
    private static List<byte[]> MINISIGN_PUBLIC_KEY_BYTES_LIST;

    private static final String MINISIGN_ALGO_DESCRIPTION = "Ed";
    private static final int MINISIGN_RANDOM_BYTES_LENGTH = 8;
    private static final int MINISIGN_ED_SIGNATURE_LENGTH = 64;
    private static final int MINISIGN_ED_PUBLIC_KEY_LENGTH = 32;

    // We use the ISO-8859-1 charset for converting between strings and bytes,
    // because there one character is exactly one byte.
    // In UTF-8, a character could be 2 or 3 bytes, in ASCII one character is 7 bits only.
    private static final Charset BYTE_DECODE_CHARSET = StandardCharsets.ISO_8859_1;


    static {
        // Init the library
        NaCl.sodium();
    }

    public SecurityService() {
        loadMinisignPublicKeys(BuildConfig.MINISIGN_SIGNATURE_VALIDATION_PUBLIC_KEY);
    }

    @VisibleForTesting
    static void loadMinisignPublicKeys(String[] publicKeys) {
        List<String> idList = new ArrayList<>();
        List<byte[]> bytesList = new ArrayList<>();
        for (String publicKey : publicKeys) {
            byte[] publicKeyRawBytes = Base64.decode(publicKey, Base64.DEFAULT);
            String publicKeyRawString = new String(publicKeyRawBytes, BYTE_DECODE_CHARSET);
            if (MINISIGN_ALGO_DESCRIPTION.length() + MINISIGN_RANDOM_BYTES_LENGTH + MINISIGN_ED_PUBLIC_KEY_LENGTH != publicKeyRawBytes.length) {
                throw new IllegalArgumentException("Invalid public key: not long enough!");
            }
            if (!MINISIGN_ALGO_DESCRIPTION.equals(publicKeyRawString.substring(0, MINISIGN_ALGO_DESCRIPTION.length()))) {
                throw new IllegalArgumentException("Unsupported algorithm, we only support '" + MINISIGN_ALGO_DESCRIPTION + "'!");
            }
            idList.add(publicKeyRawString.substring(MINISIGN_ALGO_DESCRIPTION.length(), MINISIGN_ALGO_DESCRIPTION.length() + MINISIGN_RANDOM_BYTES_LENGTH));
            bytesList.add(publicKeyRawString.substring(MINISIGN_ALGO_DESCRIPTION.length() + MINISIGN_RANDOM_BYTES_LENGTH).getBytes(BYTE_DECODE_CHARSET));
        }
        MINISIGN_PUBLIC_KEY_ID_LIST = Collections.unmodifiableList(idList);
        MINISIGN_PUBLIC_KEY_BYTES_LIST = Collections.unmodifiableList(bytesList);
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
        boolean hasMatchingPublicKeyId = false;
        for (String publicKeyId : MINISIGN_PUBLIC_KEY_ID_LIST) {
            if (publicKeyId.equals(signatureString.substring(MINISIGN_ALGO_DESCRIPTION.length(), MINISIGN_ALGO_DESCRIPTION.length() + MINISIGN_RANDOM_BYTES_LENGTH))) {
                hasMatchingPublicKeyId = true;
            }
        }
        if (!hasMatchingPublicKeyId) {
            throw new IllegalArgumentException("Signature does not match public key!");
        }

        byte[] signatureBytes = signatureString.substring(MINISIGN_ALGO_DESCRIPTION.length() + MINISIGN_RANDOM_BYTES_LENGTH).getBytes(BYTE_DECODE_CHARSET);
        for (byte[] publicKey : MINISIGN_PUBLIC_KEY_BYTES_LIST) {
            int result = Sodium.crypto_sign_verify_detached(signatureBytes, messageBytes, messageBytes.length, publicKey);
            if (result == 0) {
                return true;
            }
        }
        Log.e(TAG, "Signature validation failed!");
        return false;
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
