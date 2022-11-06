/*
 * This file is part of eduVPN.
 *
 * eduVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * eduVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package nl.eduvpn.app.viewmodel

import androidx.annotation.StringRes
import nl.eduvpn.app.R

enum class ConnectionState(@StringRes val displayString: Int?) {
        Ready(null),
        FetchingOrganizations(R.string.fetching_organization_servers),
        FetchingServerList(R.string.fetching_server_list),
        DiscoveringApi(R.string.api_discovery_message),
        FetchingProfiles(R.string.loading_available_profiles),
        Authorizing(R.string.loading_browser_for_authorization),
        ProfileDownloadingKeyPair(R.string.vpn_profile_creating_keypair)
    }
