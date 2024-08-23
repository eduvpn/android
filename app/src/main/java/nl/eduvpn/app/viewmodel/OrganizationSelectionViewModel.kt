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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.OrganizationAdapter
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.OrganizationList
import nl.eduvpn.app.entity.ServerList
import nl.eduvpn.app.service.BackendService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.OrganizationService
import nl.eduvpn.app.service.PreferencesService
import nl.eduvpn.app.service.VPNConnectionService
import javax.inject.Inject

class OrganizationSelectionViewModel @Inject constructor(
    organizationService: OrganizationService,
    preferencesService: PreferencesService,
    context: Context,
    backendService: BackendService,
    historyService: HistoryService,
    vpnConnectionService: VPNConnectionService,
) : BaseConnectionViewModel(
    context,
    backendService,
    historyService,
    preferencesService,
    vpnConnectionService,
) {

    val artworkVisible = MutableLiveData(true)

    val searchText = MutableStateFlow("")

    private val serverList: Flow<Result<ServerList>> = searchText.map { filter ->
        return@map organizationService.fetchServerList(filter)
    }

    private val secureInternetServers = serverList.map { serverList ->
        if (historyService.hasSecureInternetServer()) {
            val servers = serverList.getOrNull()?.serverList ?: return@map emptyList()
            val result: MutableList<OrganizationAdapter.OrganizationAdapterItem> = servers.filter { it.authorizationType == AuthorizationType.Distributed }
                .map {
                    OrganizationAdapter.OrganizationAdapterItem.SecureInternet(it)
                }.toMutableList()
            if (result.isNotEmpty()) {
                result.add(
                    0, OrganizationAdapter.OrganizationAdapterItem.Header(
                        R.drawable.ic_secure_internet,
                        R.string.header_secure_internet
                    )
                )
            }
            result
        } else {
            emptyList()
        }
    }

    private val instituteAccessServers = serverList.map { serverList ->
        val servers = serverList.getOrNull()?.serverList ?: return@map emptyList()
        val result: MutableList<OrganizationAdapter.OrganizationAdapterItem> = servers.filter { it.authorizationType == AuthorizationType.Local }
            .map {
                OrganizationAdapter.OrganizationAdapterItem.InstituteAccess(it)
            }.toMutableList()
        if (result.isNotEmpty()) {
            result.add(
                0, OrganizationAdapter.OrganizationAdapterItem.Header(
                    R.drawable.ic_institute,
                    R.string.header_institute_access
                )
            )
        }
        result
    }

    private val organizationList: Flow<Result<OrganizationList>> = searchText.map { filter ->
        if (historyService.hasSecureInternetServer()) {
            return@map Result.success(OrganizationList(emptyList()))
        } else {
            try {
                return@map Result.success(organizationService.fetchOrganizations(filter))
            } catch (ex: Exception) {
                return@map Result.failure(ex)
            }
        }
    }

    private val organizations: Flow<List<OrganizationAdapter.OrganizationAdapterItem>> = organizationList.map { list ->
        if (list.isFailure) {
            return@map emptyList()
        }
        val result: MutableList<OrganizationAdapter.OrganizationAdapterItem> = list.getOrNull()?.organizationList?.map {
            OrganizationAdapter.OrganizationAdapterItem.Organization(it)
        }?.toMutableList() ?: mutableListOf()
        if (result.isNotEmpty()) {
            result.add(0, OrganizationAdapter.OrganizationAdapterItem.Header(R.drawable.ic_secure_internet, R.string.header_secure_internet))
        }
        return@map result
    }

    private val addServerItem = searchText.map { filter ->
        // Search term contains at least two dots
        if (filter.count { ".".contains(it) } > 1) {
            val resultList = mutableListOf<OrganizationAdapter.OrganizationAdapterItem>()
            resultList += OrganizationAdapter.OrganizationAdapterItem.Header(
                R.drawable.ic_server,
                R.string.header_connect_your_own_server
            )
            resultList += OrganizationAdapter.OrganizationAdapterItem.AddServer(filter)
            resultList
        } else {
            emptyList()
        }
    }

    val adapterItems = combine(
        addServerItem,
        instituteAccessServers,
        organizations,
        secureInternetServers
    ) { addServerItem, instituteAccessServers, organizations, secureInternetServers ->
        addServerItem + instituteAccessServers + organizations + secureInternetServers
    }

    val noItemsFound = connectionState.switchMap { state ->
        adapterItems.asLiveData().map { items ->
            items.isEmpty() && state == ConnectionState.Ready
        }
    }

    // We do not want to show a double error in case of a connection problem, so we show the first one only
    val error = combine(serverList, organizationList) { servers, organization ->
        servers.exceptionOrNull() ?: organization.exceptionOrNull()
    }
}
