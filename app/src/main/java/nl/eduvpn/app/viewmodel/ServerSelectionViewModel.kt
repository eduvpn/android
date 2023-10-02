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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.OrganizationAdapter
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.ServerList
import nl.eduvpn.app.service.BackendService
import nl.eduvpn.app.service.EduVPNOpenVPNService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.OrganizationService
import nl.eduvpn.app.service.PreferencesService
import nl.eduvpn.app.service.SerializerService
import nl.eduvpn.app.service.VPNConnectionService
import nl.eduvpn.app.utils.Listener
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.getCountryText
import nl.eduvpn.app.utils.runCatchingCoroutine
import javax.inject.Inject

class ServerSelectionViewModel @Inject constructor(
    context: Context,
    backendService: BackendService,
    private val historyService: HistoryService,
    private val preferencesService: PreferencesService,
    eduVpnOpenVpnService: EduVPNOpenVPNService,
    private val organizationService: OrganizationService,
    vpnConnectionService: VPNConnectionService,
) : BaseConnectionViewModel(
    context,
    backendService,
    historyService,
    preferencesService,
    eduVpnOpenVpnService,
    vpnConnectionService,
), Listener {

    val adapterItems = MutableLiveData<List<OrganizationAdapter.OrganizationAdapterItem>>()

    val connectingTo = MutableLiveData<Instance>()

    // We avoid refreshing the organization too frequently.
    private val serverListCache = MutableLiveData<Pair<Long, ServerList>>()

    init {
        historyService.addListener(this)
        preferencesService.getServerList()?.let { serverList ->
            serverListCache.value = Pair(System.currentTimeMillis(), serverList)
        }
    }

    override fun onCleared() {
        super.onCleared()
        historyService.removeListener(this)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val needsServerList = historyService.addedServers?.secureInternetServer != null
        if (needsServerList && (serverListCache.value == null || System.currentTimeMillis() - serverListCache.value!!.first > SERVER_LIST_CACHE_TTL)) {
            refreshServerList()
        } else {
            refreshInstances(serverListCache.value?.second ?: ServerList(-1, emptyList()))
        }
    }


    /**
     * Refreshes the current organization, and then the instances afterwards
     */
    private fun refreshServerList() {
        connectionState.value = ConnectionState.FetchingServerList
        Log.v(TAG, "Fetching server list...")
        viewModelScope.launch(Dispatchers.IO) {
            runCatchingCoroutine {
                organizationService.fetchServerList()
            }.onSuccess { serverList ->
                Log.v(TAG, "Updated server list with latest entries.")
                serverListCache.value = Pair(System.currentTimeMillis(), serverList)
                preferencesService.setServerList(serverList)
                refreshInstances(serverList)
            }.onFailure { throwable ->
                Log.w(TAG, "Unable to fetch server list. Trying to show servers without it.", throwable)
                refreshInstances(serverListCache.value?.second
                        ?: ServerList(-1L, emptyList()))
            }

        }
    }

    /**
     * Refreshes the instances for the server selector.
     */
    private fun refreshInstances(serverList: ServerList) {
        val savedInstances = historyService.addedServers?.asInstances() ?: emptyList()
        val distributedInstance = savedInstances.firstOrNull { it.authorizationType == AuthorizationType.Distributed }
        val customServers = savedInstances.filter { it.authorizationType == AuthorizationType.Organization && it.isCustom }.sortedBy { it.sanitizedBaseURI }
        val instituteAccessItems = savedInstances.filter { it.authorizationType == AuthorizationType.Local && !it.isCustom }.sortedBy {
            it.displayName.bestTranslation ?: it.countryCode
        }
        val result = mutableListOf<OrganizationAdapter.OrganizationAdapterItem>()
        if (instituteAccessItems.isNotEmpty()) {
            result += OrganizationAdapter.OrganizationAdapterItem.Header(R.drawable.ic_institute, R.string.header_institute_access)
            result += instituteAccessItems.map { OrganizationAdapter.OrganizationAdapterItem.InstituteAccess(it) }
        }
        if (distributedInstance != null) {
            val preferredCountry = preferencesService.getPreferredCountry()
            val countryMatch = if (preferredCountry == null) {
                null
            } else {
                serverList.serverList.firstOrNull { it.authorizationType == AuthorizationType.Distributed && it.countryCode.equals(preferredCountry, ignoreCase = true) }
            }
            val displayedServer = countryMatch ?: distributedInstance
            result += OrganizationAdapter.OrganizationAdapterItem.Header(R.drawable.ic_secure_internet, R.string.header_secure_internet, includeLocationButton = true)
            result += OrganizationAdapter.OrganizationAdapterItem.SecureInternet(displayedServer, null)
        }
        if (customServers.isNotEmpty()) {
            result += OrganizationAdapter.OrganizationAdapterItem.Header(R.drawable.ic_server, R.string.header_other_servers)
            result += customServers.map { OrganizationAdapter.OrganizationAdapterItem.InstituteAccess(it) }
        }
        adapterItems.postValue(result)
        connectionState.postValue(ConnectionState.Ready)
    }

    override fun update(o: Any, arg: Any?) {
        if (o is HistoryService) {
            refresh()
        }
    }

    fun requestCountryList(): List<Pair<Instance, String>>? {
        val allInstances = serverListCache.value?.second?.serverList
        return allInstances?.filter {
            it.authorizationType == AuthorizationType.Distributed && it.countryCode != null
        }?.map {
            Pair(it, it.getCountryText() ?: "Unknown country")
        }
    }

    fun changePreferredCountry(selectedInstance: Instance) {
        preferencesService.setPreferredCountry(selectedInstance.countryCode)
        refresh()
    }

    fun hasNoMoreServers(): Boolean {
        return historyService.addedServers?.hasServers() != true
    }

    companion object {
        private val TAG = ServerSelectionViewModel::class.java.name
        private const val SERVER_LIST_CACHE_TTL = 15 * 60 * 1_000L // 15 minutes
    }
}
