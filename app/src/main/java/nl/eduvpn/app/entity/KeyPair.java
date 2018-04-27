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

import android.util.Base64;

import javax.security.cert.X509Certificate;

import nl.eduvpn.app.utils.Log;

/**
 * Keypair which is created by the API and sent to the user.
 * Created by Daniel Zolnai on 2017-08-01.
 */
public class KeyPair {

    private static final String TAG = KeyPair.class.getName();

    private boolean _isOK;
    private String _certificate;
    private String _privateKey;

    public KeyPair(boolean isOK, String certificate, String privateKey) {
        _isOK = isOK;
        _certificate = certificate;
        _privateKey = privateKey;
    }


    public boolean isOK() {
        return _isOK;
    }

    public String getCertificate() {
        return _certificate;
    }

    public String getPrivateKey() {
        return _privateKey;
    }

    public String getCertificateCommonName() {
        try {
            // Remove header and footer
            String base64Encoded =_certificate
                    .replace("-----BEGIN CERTIFICATE-----\n", "")
                    .replace("-----END CERTIFICATE-----", "");
            byte[] certificateData = Base64.decode(base64Encoded, Base64.DEFAULT);
            String commonName = X509Certificate.getInstance(certificateData).getSubjectDN().getName();
            if (commonName.startsWith("CN=")) {
                return commonName.replace("CN=", "");
            } else {
                return commonName;
            }
        } catch (Exception ex) {
            Log.e(TAG, "Exception while parsing certicificate", ex);
            return null;
        }
    }
}
