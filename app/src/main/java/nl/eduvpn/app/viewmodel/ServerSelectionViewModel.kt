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

import android.content.Context
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.service.*
import nl.eduvpn.app.utils.Log

class ServerSelectionViewModel(private val context: Context,
                               apiService: APIService,
                               serializerService: SerializerService,
                               configurationService: ConfigurationService,
                               private val historyService: HistoryService,
                               preferencesService: PreferencesService,
                               connectionService: ConnectionService,
                               vpnService: VPNService,
                               private val organizationService: OrganizationService) : ConnectionViewModel(
        context, apiService,
        serializerService,
        configurationService,
        historyService,
        preferencesService,
        connectionService,
        vpnService) {

    override fun onResume() {
        super.onResume()
        val organization = historyService.savedOrganization ?: return
        disposables.add(organizationService.getInstanceListForOrganization(organization.organization)
                .subscribe({ latestServers ->
                    // Organization servers seem to be OK. Check if our instance is also part of it.
                    val instances = historyService.savedAuthStateList
                            .map { it.instance }
                            .filter { it.authorizationType == AuthorizationType.Organization }
                    // Finding a match
                    if (instances.size != 1) {
                        Log.w(TAG, "An organization should only have one instance!")
                    }
                    val appInstance = instances.getOrNull(0)
                    if (appInstance == null) {
                        Log.w(TAG, "No organization instance found!")
                        return@subscribe
                    }
                    val foundInLatestServers = latestServers.any { it.sanitizedBaseURI == appInstance.sanitizedBaseURI }
                    if (!foundInLatestServers) {
                        warning.value = context.getString(R.string.error_server_not_found_in_organization, appInstance.displayName)
                    }
                }, { throwable ->
                    if (throwable is OrganizationService.OrganizationDeletedException) {
                        warning.value = context.getString(R.string.error_organization_not_listed)
                    } else {
                        Log.i(TAG, "Unable to refresh organization servers.")
                    }
                })
        )
    }

    companion object {
        private val TAG = ServerSelectionViewModel::class.java.name
    }
}