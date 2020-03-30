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
import androidx.lifecycle.MutableLiveData
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.DiscoveredInstance
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.SavedOrganization
import nl.eduvpn.app.service.*
import nl.eduvpn.app.utils.Log
import java.util.Observable
import java.util.Observer

class ServerSelectionViewModel(private val context: Context,
                               apiService: APIService,
                               serializerService: SerializerService,
                               private val configurationService: ConfigurationService,
                               private val historyService: HistoryService,
                               preferencesService: PreferencesService,
                               connectionService: ConnectionService,
                               vpnService: VPNService,
                               private val organizationService: OrganizationService) : ConnectionViewModel(
        context, apiService,
        serializerService,
        historyService,
        preferencesService,
        connectionService,
        vpnService), Observer {

    val instances = MutableLiveData<List<DiscoveredInstance>>()


    init {
        if (BuildConfig.API_DISCOVERY_ENABLED) {
            configurationService.addObserver(this)
        }
        historyService.addObserver(this)
    }

    override fun onCleared() {
        super.onCleared()
        if (BuildConfig.API_DISCOVERY_ENABLED) {
            configurationService.deleteObserver(this)
        }
        historyService.deleteObserver(this)
    }


    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val organization = historyService.savedOrganization
        if (organization == null) {
            refreshInstances()
        } else {
            refreshOrganization(organization)
        }
    }

    /**
     * Refreshes the current organization, and then the instances afterwards
     */
    private fun refreshOrganization(organization: SavedOrganization) {
        disposables.add(organizationService.getInstanceListForOrganization(organization.organization)
                .subscribe({ currentInstances ->
                    // Organization servers seem to be OK. Check if our instance is also part of it.
                    val oldInstances = historyService.savedAuthStateList
                            .map { it.instance }
                            .filter { it.authorizationType == AuthorizationType.Organization }
                    // Checking if there are missing ones
                    val newInstanceUrls = currentInstances.map { it.sanitizedBaseURI }.toSet()
                    val missingInstances = oldInstances.filter { it.sanitizedBaseURI !in newInstanceUrls }

                    if (missingInstances.size == 1) {
                        warning.value = context.getString(R.string.error_server_not_found_in_organization_single, missingInstances[0].displayName)
                    } else if (missingInstances.size > 1) {
                        warning.value = context.getString(R.string.error_server_not_found_in_organization_multiple, missingInstances.joinToString(separator = ", ") { it.displayName })
                    }
                    // Old and new:
                    val allInstances =
                            currentInstances.map { discoveredInstanceFromOldAndNew(it, oldInstances) }
                    missingInstances.map {
                        DiscoveredInstance(it, true, it.peerList?.map { false } ?: emptyList())
                    }
                    refreshInstances(allInstances)
                }, { throwable ->
                    if (throwable is OrganizationService.OrganizationDeletedException) {
                        warning.value = context.getString(R.string.error_organization_not_listed)
                    } else {
                        Log.i(TAG, "Unable to refresh organization servers.")
                    }
                    refreshInstances(organization.servers.map { DiscoveredInstance(it, true) })
                })
        )
    }

    private fun discoveredInstanceFromOldAndNew(currentInstance: Instance, oldInstances: List<Instance>): DiscoveredInstance {
        val newPeerList = currentInstance.peerList ?: emptyList()
        val oldInstance = oldInstances.firstOrNull { it.sanitizedBaseURI == currentInstance.sanitizedBaseURI }
                ?: return DiscoveredInstance(currentInstance, false, currentInstance.peerList?.map { false }
                        ?: emptyList())

        val newPeerUrls = newPeerList.map { it.sanitizedBaseURI }.toSet()
        val missingPeers = oldInstance.peerList?.filter { it.sanitizedBaseURI !in newPeerUrls }
                ?: emptyList()

        val displayPeers = newPeerList + missingPeers
        val cacheStatuses = newPeerList.map { false } + missingPeers.map { true }

        return DiscoveredInstance(currentInstance.copy(peerList = displayPeers), false, cacheStatuses)
    }

    /**
     * Refreshes the instances for the server selector. Also downloads the instance groups.
     */
    private fun refreshInstances(organizationInstances: List<DiscoveredInstance> = emptyList()) {
        val nonOrganizationInstances = historyService.savedAuthStateList.map { it.instance }.filter { it.authorizationType != AuthorizationType.Organization }
        val allInstances = nonOrganizationInstances.map {
            DiscoveredInstance(it, false)
        } + organizationInstances

        instances.value = allInstances.sortedWith(compareBy { it.instance.peerList?.isNotEmpty() == true })
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o is HistoryService) {
            refresh()
        } else if (o is ConfigurationService) {
            refresh()
        }
    }


    companion object {
        private val TAG = ServerSelectionViewModel::class.java.name
    }
}