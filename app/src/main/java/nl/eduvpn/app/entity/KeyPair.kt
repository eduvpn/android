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
package nl.eduvpn.app.entity

import android.util.Base64
import nl.eduvpn.app.utils.Log
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Keypair which is created by the API and sent to the user.
 * Created by Daniel Zolnai on 2017-08-01.
 */
data class KeyPair(val isOK: Boolean, val certificate: String, val privateKey: String) {

    // Remove header and footer
    private val x509Certificate: X509Certificate? by lazy {
        try {
            // Remove header and footer
            val base64Encoded = certificate
                .replace("-----BEGIN CERTIFICATE-----\n", "")
                .replace("-----END CERTIFICATE-----", "")
            val certificateData = Base64.decode(base64Encoded, Base64.DEFAULT)
            val cf = CertificateFactory.getInstance("X.509")
            cf.generateCertificate(ByteArrayInputStream(certificateData)) as X509Certificate
        } catch (ex: Exception) {
            Log.e(TAG, "Unable to parse certificate!", ex)
            null
        }
    }

    val certificateCommonName: String? by lazy {
        x509Certificate?.let { certificate ->
            val commonName = certificate.subjectDN.name
            if (commonName.startsWith("CN=")) {
                commonName.replace("CN=", "")
            } else {
                commonName
            }
        }
    }

    val expiryTimeMillis: Long? by lazy {
        x509Certificate?.notAfter?.time
    }

    companion object {
        private val TAG = KeyPair::class.java.name
    }
}
