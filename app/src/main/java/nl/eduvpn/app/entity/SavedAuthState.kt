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

import net.openid.appauth.AuthState

/**
 * Stores the mapping between the base URI and the authorization state containing the current and refresh tokens.
 * Created by Daniel Zolnai on 2016-10-20.
 *
 * @param instance  The VPN provider the token is valid for.
 * @param authState The authorization state with the tokens.
 */
data class SavedAuthState(val instance: Instance, var authState: AuthState)
