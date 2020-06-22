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
import nl.eduvpn.app.Constants
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.OrganizationAdapter
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Organization
import nl.eduvpn.app.service.*
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.getCountryText
import java.util.Locale
import java.util.Observable
import java.util.Observer
import javax.inject.Inject

class ServerSelectionViewModel @Inject constructor(
        context: Context,
        apiService: APIService,
        serializerService: SerializerService,
        private val historyService: HistoryService,
        private val preferencesService: PreferencesService,
        connectionService: ConnectionService,
        vpnService: VPNService,
        private val organizationService: OrganizationService) : BaseConnectionViewModel(
        context, apiService,
        serializerService,
        historyService,
        preferencesService,
        connectionService,
        vpnService), Observer {


    val adapterItems = MutableLiveData<List<OrganizationAdapter.OrganizationAdapterItem>>()

    // We avoid refreshing the organization too frequently.
    private val serverListCache = MutableLiveData<Pair<Long, List<Instance>>>()


    init {
        historyService.addObserver(this)
    }

    override fun onCleared() {
        super.onCleared()
        historyService.deleteObserver(this)
    }


    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        if (serverListCache.value == null || System.currentTimeMillis() - serverListCache.value!!.first > SERVER_LIST_CACHE_TTL) {
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
        val customServers = savedInstances.filter { it.authorizationType == AuthorizationType.Local && it.isCustom }.sortedBy { it.sanitizedBaseURI }
        val instituteAccessItems = savedInstances.filter { it.authorizationType == AuthorizationType.Local && !it.isCustom }.sortedBy {
            it.displayName ?: it.countryCode
        }
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
            result += OrganizationAdapter.OrganizationAdapterItem.Header(R.drawable.ic_secure_internet, R.string.header_secure_internet, includeLocationButton = true)
            result += OrganizationAdapter.OrganizationAdapterItem.SecureInternet(displayedServer, null)
        }
        if (customServers.isNotEmpty()) {
            result += OrganizationAdapter.OrganizationAdapterItem.Header(R.drawable.ic_server, R.string.header_other_servers)
            result += customServers.map { OrganizationAdapter.OrganizationAdapterItem.InstituteAccess(it) }
        }
        adapterItems.value = result
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o is HistoryService) {
            refresh()
        }
    }

    fun requestCountryList(): List<Pair<Instance, String>>? {
        val allInstances = serverListCache.value?.second
        return allInstances?.filter {
            it.authorizationType == AuthorizationType.Distributed && it.countryCode != null
        }?.map {
            Pair(it, it.getCountryText() ?: "Unknown country")
        }
    }

    fun changePreferredCountry(selectedInstance: Instance) {
        preferencesService.preferredCountry = selectedInstance.countryCode
        refresh()
    }

    fun saveOrganization(organization: Organization) {
        preferencesService.currentOrganization = organization
    }

    companion object {
        private val TAG = ServerSelectionViewModel::class.java.name
        private const val SERVER_LIST_CACHE_TTL = 15 * 60 * 1_000L // 15 minutes
    }
}