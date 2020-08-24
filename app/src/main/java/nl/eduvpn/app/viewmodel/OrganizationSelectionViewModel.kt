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
import androidx.lifecycle.Transformations
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.OrganizationAdapter
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Organization
import nl.eduvpn.app.entity.OrganizationList
import nl.eduvpn.app.entity.ServerList
import nl.eduvpn.app.service.APIService
import nl.eduvpn.app.service.ConnectionService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.OrganizationService
import nl.eduvpn.app.service.PreferencesService
import nl.eduvpn.app.service.SerializerService
import nl.eduvpn.app.service.VPNService
import nl.eduvpn.app.utils.Log
import javax.inject.Inject

class OrganizationSelectionViewModel @Inject constructor(
        organizationService: OrganizationService,
        private val preferencesService: PreferencesService,
        context: Context,
        apiService: APIService,
        serializerService: SerializerService,
        historyService: HistoryService,
        connectionService: ConnectionService,
        vpnService: VPNService) : BaseConnectionViewModel(context, apiService, serializerService, historyService, preferencesService, connectionService, vpnService) {

    val state = MutableLiveData<ConnectionState>().also { it.value = ConnectionState.Ready }

    private val organizations = MutableLiveData<List<Organization>>()
    private val servers = MutableLiveData<List<Instance>>()

    val artworkVisible = MutableLiveData(true)

    val searchText = MutableLiveData("")

    init {
        val getOrganizationsCall = if (historyService.savedOrganization == null) {
            state.value = ConnectionState.FetchingOrganizations
            organizationService.fetchOrganizations()
        } else {
            // We can't show any organization servers, user needs to reset to switch.
            state.value = ConnectionState.FetchingServerList
            Single.just(OrganizationList(-1L, emptyList()))
        }
        val cachedServerList = preferencesService.serverList
        val getServerListCall = if (cachedServerList != null) {
            Single.just(cachedServerList)
        } else {
            organizationService.fetchServerList()
        }
        disposables.add(
                Single.zip(getOrganizationsCall, getServerListCall, BiFunction { orgList: OrganizationList, serverList: ServerList ->
                    Pair(orgList, serverList)
                }).subscribe({ organizationServerListPair ->
                    val organizationList = organizationServerListPair.first
                    val serverList = organizationServerListPair.second

                    val lastKnownOrganizationVersion = preferencesService.lastKnownOrganizationListVersion
                    val lastKnownServerListVersion = preferencesService.lastKnownServerListVersion

                    if (serverList.version > 0 && lastKnownServerListVersion != null && lastKnownServerListVersion > serverList.version) {
                        organizations.value = emptyList()
                        servers.value = emptyList()
                        state.value = ConnectionState.Ready
                        parentAction.value = ParentAction.DisplayError(R.string.error_server_list_version_check_title, context.getString(R.string.error_server_list_version_check_message))
                        return@subscribe
                    } else if (organizationList.version > 0 && lastKnownOrganizationVersion != null && lastKnownOrganizationVersion > organizationList.version) {
                        organizations.value = emptyList()
                        servers.value = serverList.serverList
                        state.value = ConnectionState.Ready
                        parentAction.value = ParentAction.DisplayError(R.string.error_organization_list_version_check_title, context.getString(R.string.error_organization_list_version_check_message))
                        return@subscribe
                    }

                    if (organizationList.version > 0) {
                        preferencesService.lastKnownOrganizationListVersion = organizationList.version
                    }
                    if (serverList.version > 0) {
                        preferencesService.lastKnownServerListVersion = serverList.version
                    }

                    organizations.value = organizationList.organizationList
                    servers.value = serverList.serverList
                    state.value = ConnectionState.Ready
                }, { throwable ->
                    Log.w(TAG, "Unable to fetch organization list!", throwable)
                    parentAction.value = ParentAction.DisplayError(R.string.error_fetching_organizations, throwable.toString())
                    state.value = ConnectionState.Ready
                })
        )
    }

    val adapterItems = Transformations.switchMap(organizations) { organizations ->
        Transformations.switchMap(servers) { servers ->
            Transformations.map(searchText) { searchText ->
                val resultList = mutableListOf<OrganizationAdapter.OrganizationAdapterItem>()
                // Search contains at least two dots
                if (searchText.count { ".".contains(it) } > 1) {
                    resultList += OrganizationAdapter.OrganizationAdapterItem.Header(R.drawable.ic_server, R.string.header_connect_your_own_server)
                    resultList += OrganizationAdapter.OrganizationAdapterItem.AddServer(searchText)
                    return@map resultList
                }
                val instituteAccessServers = servers.filter {
                    it.authorizationType == AuthorizationType.Local && (searchText.isNullOrBlank() || it.displayName?.contains(searchText, ignoreCase = true) == true)
                }.sortedBy { it.displayName }
                        .map { OrganizationAdapter.OrganizationAdapterItem.InstituteAccess(it) }
                val secureInternetServers = organizations.filter {
                    if (searchText.isNullOrBlank()) {
                        true
                    } else {
                        it.displayName.translations.any { keyValue -> keyValue.value.contains(searchText, ignoreCase = true) } ||
                                it.keywordList.translations.any { keyValue -> keyValue.value.contains(searchText, ignoreCase = true) }
                    }
                }.mapNotNull { organization ->
                    val matchingServer = servers
                            .firstOrNull {
                                it.authorizationType == AuthorizationType.Distributed &&
                                        it.baseURI == organization.secureInternetHome
                            }
                    if (matchingServer != null) {
                        OrganizationAdapter.OrganizationAdapterItem.SecureInternet(matchingServer, organization)
                    } else {
                        null
                    }
                }
                if (instituteAccessServers.isNotEmpty()) {
                    resultList += OrganizationAdapter.OrganizationAdapterItem.Header(R.drawable.ic_institute, R.string.header_institute_access)
                    resultList += instituteAccessServers
                }
                if (secureInternetServers.isNotEmpty()) {
                    resultList += OrganizationAdapter.OrganizationAdapterItem.Header(R.drawable.ic_secure_internet, R.string.header_secure_internet)
                    resultList += secureInternetServers
                }
                resultList
            }
        }
    }

    val noItemsFound = Transformations.switchMap(state) { state ->
        Transformations.map(adapterItems) { items ->
            items.isEmpty() && state == ConnectionState.Ready
        }
    }


    fun selectOrganizationAndInstance(organization: Organization?, instance: Instance) {
        preferencesService.currentOrganization = organization
        discoverApi(instance)
    }

    companion object {
        private val TAG = OrganizationSelectionViewModel::class.java.name
    }
}