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
import nl.eduvpn.app.adapter.OrganizationAdapter
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.service.*
import nl.eduvpn.app.utils.Log
import java.util.Observable
import java.util.Observer
import javax.inject.Inject

class ServerSelectionViewModel @Inject constructor(
        private val context: Context,
        apiService: APIService,
        serializerService: SerializerService,
        private val configurationService: ConfigurationService,
        private val historyService: HistoryService,
        private val preferencesService: PreferencesService,
        connectionService: ConnectionService,
        vpnService: VPNService,
        private val organizationService: OrganizationService) : ConnectionViewModel(
        context, apiService,
        serializerService,
        historyService,
        preferencesService,
        connectionService,
        vpnService), Observer {


    val adapterItems = MutableLiveData<List<OrganizationAdapter.OrganizationAdapterItem>>()

    // We avoid refreshing the organization too frequently.
    val serverListCache = MutableLiveData<Pair<Long, List<Instance>>>()


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
        } else if (serverListCache.value == null || System.currentTimeMillis() - serverListCache.value!!.first > SERVER_LIST_CACHE_TTL) {
            refreshServerList()
        } else {
            refreshInstances(serverListCache.value!!.second)
        }
    }

    /**
     * Refreshes the current organization, and then the instances afterwards
     */
    private fun refreshServerList() {
        disposables.add(organizationService.fetchServerList()
                .subscribe({
                    serverListCache.value = Pair(System.currentTimeMillis(), it)
                    refreshInstances(it)
                }, {
                    Log.w(TAG, "Unable to fetch server list. Trying to show servers without it.", it)
                    refreshInstances(serverListCache.value?.second ?: emptyList())
                })
        )
    }

    /**
     * Refreshes the instances for the server selector.
     */
    private fun refreshInstances(serverList: List<Instance> = emptyList()) {
        val savedInstances = historyService.savedAuthStateList.map { it.instance }
        val distributedInstance = savedInstances.firstOrNull { it.authorizationType == AuthorizationType.Distributed }
        val instituteAccessItems = savedInstances.filter { it.authorizationType == AuthorizationType.Local }
        val result = mutableListOf<OrganizationAdapter.OrganizationAdapterItem>()
        if (instituteAccessItems.isNotEmpty()) {
            result += OrganizationAdapter.OrganizationAdapterItem.Header(R.drawable.ic_institute, R.string.header_institute_access)
            result += instituteAccessItems.map { OrganizationAdapter.OrganizationAdapterItem.InstituteAccess(it) }
        }
        if (distributedInstance != null) {
            val preferredCountry = preferencesService.preferredCountry
            val countryMatch = if (preferencesService.preferredCountry == null) {
                null
            } else {
                serverList.firstOrNull { it.authorizationType == AuthorizationType.Distributed && it.countryCode.equals(preferredCountry, ignoreCase = true) }
            }
            val displayedServer = countryMatch ?: distributedInstance
            result += OrganizationAdapter.OrganizationAdapterItem.Header(R.drawable.ic_secure_internet, R.string.header_secure_internet)
            result += OrganizationAdapter.OrganizationAdapterItem.SecureInternet(displayedServer, null)
        }
        adapterItems.value = result
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
        private const val SERVER_LIST_CACHE_TTL = 15 * 60 * 1_000L // 15 minutes
    }
}