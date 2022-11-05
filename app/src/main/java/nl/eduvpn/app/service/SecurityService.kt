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
package nl.eduvpn.app.service

import android.util.Base64
import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.utils.Log
import org.libsodium.jni.NaCl
import org.libsodium.jni.Sodium
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

/**
 * Service which is responsible for all things related to security.
 *
 *
 * Created by Daniel Zolnai on 2017-08-01.
 */
class SecurityService() {
    companion object {
        private val TAG = SecurityService::class.java.name

        // Public key ID is used as map keys, public key bytes are used as map values.
        private lateinit var MINISIGN_PUBLIC_KEYS: Map<String, ByteArray> //todo: val

        private const val MINISIGN_ALGO_DESCRIPTION_LEGACY = "Ed"
        private const val MINISIGN_ALGO_DESCRIPTION_HASHED = "ED"
        private const val MINISIGN_RANDOM_BYTES_LENGTH = 8
        private const val MINISIGN_ED_SIGNATURE_LENGTH = 64
        private const val MINISIGN_ED_PUBLIC_KEY_LENGTH = 32

        // We use the ISO-8859-1 charset for converting between strings and bytes,
        // because there one character is exactly one byte.
        // In UTF-8, a character could be 2 or 3 bytes, in ASCII one character is 7 bits only.
        private val BYTE_DECODE_CHARSET = StandardCharsets.ISO_8859_1

        @VisibleForTesting
        fun loadMinisignPublicKeys(publicKeys: Array<String>) {
            val publicKeyRawStringList = publicKeys.map { publicKey ->
                val publicKeyRawBytes = Base64.decode(publicKey, Base64.DEFAULT)
                val publicKeyRawString = String(publicKeyRawBytes, BYTE_DECODE_CHARSET)
                require(
                    MINISIGN_ALGO_DESCRIPTION_LEGACY.length + MINISIGN_RANDOM_BYTES_LENGTH
                            + MINISIGN_ED_PUBLIC_KEY_LENGTH == publicKeyRawBytes.size
                ) { "Invalid public key: not long enough!" }
                val algorithm =
                    publicKeyRawString.substring(0, MINISIGN_ALGO_DESCRIPTION_LEGACY.length)
                require(
                    algorithm == MINISIGN_ALGO_DESCRIPTION_LEGACY || algorithm == MINISIGN_ALGO_DESCRIPTION_HASHED
                ) {
                    "Unsupported algorithm, we only support '$MINISIGN_ALGO_DESCRIPTION_LEGACY'" +
                            " and '$MINISIGN_ALGO_DESCRIPTION_HASHED'!"
                }
                publicKeyRawString
            }
            MINISIGN_PUBLIC_KEYS = publicKeyRawStringList.map { publicKeyRawString ->
                val publicKeyId = publicKeyRawString.substring(
                    MINISIGN_ALGO_DESCRIPTION_LEGACY.length,
                    MINISIGN_ALGO_DESCRIPTION_LEGACY.length + MINISIGN_RANDOM_BYTES_LENGTH
                )
                val publicKeyBytes = publicKeyRawString
                    .substring(MINISIGN_ALGO_DESCRIPTION_LEGACY.length + MINISIGN_RANDOM_BYTES_LENGTH)
                    .toByteArray(BYTE_DECODE_CHARSET)
                Pair(publicKeyId, publicKeyBytes)
            }.toMap()
        }

        init {
            // Init the library
            NaCl.sodium()
        }
    }

    init {
        loadMinisignPublicKeys(BuildConfig.MINISIGN_SIGNATURE_VALIDATION_PUBLIC_KEY)
    }

    private fun hashMessage(messageBytes: ByteArray): ByteArray {
        val hash = ByteArray(Sodium.crypto_generichash_bytes_max())
        val key = ByteArray(0)
        Sodium.crypto_generichash(hash, hash.size, messageBytes, messageBytes.size, key, key.size)
        return hash
    }

    @CheckResult
    @Throws(IllegalArgumentException::class)
    fun verifyMinisign(message: ByteArray, signatureBase64: String): Boolean {
        val signatureData = getSecondLine(signatureBase64)
        val signatureBytesWithMetadata = Base64.decode(signatureData, Base64.DEFAULT)
        val signatureString = String(signatureBytesWithMetadata, BYTE_DECODE_CHARSET)

        require(
            MINISIGN_ALGO_DESCRIPTION_LEGACY.length + MINISIGN_RANDOM_BYTES_LENGTH
                    + MINISIGN_ED_SIGNATURE_LENGTH == signatureBytesWithMetadata.size
        ) { "Invalid signature: not long enough!" }

        val publicKeyID = signatureString.substring(
            MINISIGN_ALGO_DESCRIPTION_LEGACY.length,
            MINISIGN_ALGO_DESCRIPTION_LEGACY.length + MINISIGN_RANDOM_BYTES_LENGTH
        )
        val publicKeyBytes = MINISIGN_PUBLIC_KEYS.getOrElse(publicKeyID) {
            throw IllegalArgumentException("Untrusted public key!")
        }

        val signatureBytes = signatureString
            .substring(MINISIGN_ALGO_DESCRIPTION_LEGACY.length + MINISIGN_RANDOM_BYTES_LENGTH)
            .toByteArray(BYTE_DECODE_CHARSET)

        val algorithm =
            signatureString.substring(0, MINISIGN_ALGO_DESCRIPTION_LEGACY.length)
        val signedBytes = when (algorithm) {
            MINISIGN_ALGO_DESCRIPTION_LEGACY -> message
            MINISIGN_ALGO_DESCRIPTION_HASHED -> hashMessage(message)
            else -> throw IllegalArgumentException(
                "Unsupported algorithm, we only support '$MINISIGN_ALGO_DESCRIPTION_LEGACY'" +
                        " and '$MINISIGN_ALGO_DESCRIPTION_HASHED'!"
            )
        }
        val result = Sodium.crypto_sign_verify_detached(
            signatureBytes,
            signedBytes,
            signedBytes.size,
            publicKeyBytes
        )
        if (result == 0) {
            return true
        }
        Log.e(TAG, "Signature validation failed!")
        return false
    }

    /**
     * Generates a cryptographically secure random string using Java's SecureRandom class.
     *
     * @return A string containing Base64 encoded random bytes.
     */
    suspend fun generateSecureRandomString(maxLength: Int?): String {
        val random = SecureRandom()
        val randomBytes = ByteArray(128)
        withContext(Dispatchers.IO) {
            random.nextBytes(randomBytes)
        }
        // We use Base64 to convert random bytes into a string representation, NOT for encryption
        var base64 = Base64.encodeToString(randomBytes, Base64.DEFAULT)
        // Strip all newlines and spaces (note: the first param is Regexp)
        base64 = base64.replace("\n ".toRegex(), "")
        if (maxLength != null && maxLength > 0) {
            base64 = base64.substring(0, Math.min(maxLength, base64.length))
        }
        return base64
    }

    @VisibleForTesting
    fun getSecondLine(input: String): String {
        val splitLines = input.split(System.lineSeparator())
        return splitLines.getOrElse(1) { throw IllegalArgumentException("Input has less than 2 lines!") }
    }
}
