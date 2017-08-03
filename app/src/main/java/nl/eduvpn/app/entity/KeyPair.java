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

/**
 * Keypair which is created by the API and sent to the user.
 * Created by Daniel Zolnai on 2017-08-01.
 */
public class KeyPair {

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
}
