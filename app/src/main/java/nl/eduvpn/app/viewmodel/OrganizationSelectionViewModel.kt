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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.OrganizationAdapter
import nl.eduvpn.app.entity.AuthorizationType
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Organization
import nl.eduvpn.app.entity.OrganizationList
import nl.eduvpn.app.entity.ServerList
import nl.eduvpn.app.entity.TranslatableString
import nl.eduvpn.app.service.BackendService
import nl.eduvpn.app.service.EduVPNOpenVPNService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.OrganizationService
import nl.eduvpn.app.service.PreferencesService
import nl.eduvpn.app.service.SerializerService
import nl.eduvpn.app.service.VPNConnectionService
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.runCatchingCoroutine
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

class OrganizationSelectionViewModel @Inject constructor(
    organizationService: OrganizationService,
    private val preferencesService: PreferencesService,
    context: Context,
    backendService: BackendService,
    historyService: HistoryService,
    eduVpnOpenVpnService: EduVPNOpenVPNService,
    vpnConnectionService: VPNConnectionService,
) : BaseConnectionViewModel(
    context,
    backendService,
    historyService,
    preferencesService,
    eduVpnOpenVpnService,
    vpnConnectionService,
) {

    val state = MutableLiveData<ConnectionState>().also { it.value = ConnectionState.Ready }

    private val organizations = MutableLiveData<List<Organization>>()
    private val instituteAccessServers =
        MutableLiveData<List<OrganizationAdapter.OrganizationAdapterItem.InstituteAccess>>()
    private val secureInternetServers =
        MutableLiveData<List<Instance>>()

    val artworkVisible = MutableLiveData(true)

    val searchText = MutableLiveData("")

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // We want to be able to handle async failures, so use supervisorScope
            // https://kotlinlang.org/docs/reference/coroutines/exception-handling.html#supervision
            supervisorScope {
                val organizationListDeferred = if (historyService.organizationList == null) {
                    state.postValue(ConnectionState.FetchingOrganizations)
                    async {
                        val organizationList = organizationService.fetchOrganizations()
                        historyService.organizationList = organizationList
                        organizationList
                    }
                } else {
                    // We can't show any organization servers, user needs to reset to switch.
                    state.postValue(ConnectionState.FetchingServerList)
                    CompletableDeferred(OrganizationList(-1L, emptyList()))
                }
                val cachedServerList = preferencesService.getServerList()
                val serverListDeferred = if (cachedServerList != null) {
                    CompletableDeferred(cachedServerList)
                } else {
                    async { organizationService.fetchServerList() }
                }

                val lastKnownOrganizationVersion =
                    preferencesService.getLastKnownOrganizationListVersion()
                val lastKnownServerListVersion = preferencesService.getLastKnownServerListVersion()

                val organizationList =
                    runCatchingCoroutine() { organizationListDeferred.await() }.getOrElse {
                        Log.w(TAG, "Organizations call has failed!", it)
                        OrganizationList(-1L, emptyList())
                    }

                val serverList = runCatchingCoroutine { serverListDeferred.await() }.getOrElse {
                    Log.w(TAG, "Server list call has failed!", it)
                    ServerList(-1L, emptyList())
                }
                println("SERVERLIST: " + serverList.serverList[0].authorizationType)
                if (serverList.version > 0 && lastKnownServerListVersion != null && lastKnownServerListVersion > serverList.version) {
                    organizations.value = emptyList()
                    instituteAccessServers.value = emptyList()
                    secureInternetServers.value = emptyList()
                    state.value = ConnectionState.Ready
                    _parentAction.value = ParentAction.DisplayError(
                        R.string.error_server_list_version_check_title,
                        context.getString(R.string.error_server_list_version_check_message)
                    )
                } else if (organizationList.version > 0 && lastKnownOrganizationVersion != null && lastKnownOrganizationVersion > organizationList.version) {
                    organizations.value = emptyList()
                    instituteAccessServers.value = emptyList()
                    secureInternetServers.value = emptyList()
                    state.value = ConnectionState.Ready
                    _parentAction.value = ParentAction.DisplayError(
                        R.string.error_organization_list_version_check_title,
                        context.getString(R.string.error_organization_list_version_check_message)
                    )
                }

                if (organizationList.version > 0) {
                    preferencesService.setLastKnownOrganizationListVersion(organizationList.version)
                }
                if (serverList.version > 0) {
                    preferencesService.setLastKnownServerListVersion(serverList.version)
                }

                val sortedOrganizations = organizationList.organizationList.sortedWith(
                    Comparator.comparing(
                        { i -> i.displayName.bestTranslation },
                        Collator.getInstance(Locale.getDefault())
                    )
                )

                val sortedInstituteAccessServers = serverList.serverList.filter {
                    it.authorizationType == AuthorizationType.Local
                }.sortedWith(
                    Comparator.comparing(
                        { i -> i.displayName.bestTranslation },
                        Collator.getInstance(Locale.getDefault())
                    )
                ).map { OrganizationAdapter.OrganizationAdapterItem.InstituteAccess(it) }

                val secureInternetServerList = serverList.serverList.filter {
                    it.authorizationType == AuthorizationType.Distributed
                }

                organizations.postValue(sortedOrganizations)
                instituteAccessServers.postValue(sortedInstituteAccessServers)
                secureInternetServers.postValue(secureInternetServerList)
                state.postValue(ConnectionState.Ready)
            }
        }
    }

    private fun matchesServer(
        searchText: String,
        displayName: TranslatableString,
        keywords: TranslatableString?
    ): Boolean {
        return searchText.isBlank() || displayName.translations.any { keyValue ->
            keyValue.value.contains(searchText, ignoreCase = true)
        } || (keywords != null && keywords.translations.any { keyValue ->
            keyValue.value.contains(searchText, ignoreCase = true)
        })
    }

    val adapterItems = Transformations.switchMap(organizations) { organizations ->
        Transformations.switchMap(instituteAccessServers) { instituteAccessServers ->
            Transformations.switchMap(secureInternetServers) { secureInternetServers ->
                Transformations.map(searchText) { searchText ->
                    val resultList = mutableListOf<OrganizationAdapter.OrganizationAdapterItem>()
                    // Search contains at least two dots
                    if (searchText.count { ".".contains(it) } > 1) {
                        resultList += OrganizationAdapter.OrganizationAdapterItem.Header(
                            R.drawable.ic_server,
                            R.string.header_connect_your_own_server
                        )
                        resultList += OrganizationAdapter.OrganizationAdapterItem.AddServer(
                            searchText
                        )
                        return@map resultList
                    }
                    val instituteAccessServersFiltered = instituteAccessServers.filter {
                        matchesServer(searchText, it.server.displayName, it.server.keywords)
                    }
                    val secureInternetServersFiltered = organizations.filter {
                        matchesServer(searchText, it.displayName, it.keywordList)
                    }.mapNotNull { organization ->
                        val matchingServer = secureInternetServers
                            .firstOrNull {
                                it.baseURI == organization.secureInternetHome
                            }
                        if (matchingServer != null) {
                            OrganizationAdapter.OrganizationAdapterItem.SecureInternet(
                                matchingServer,
                                organization
                            )
                        } else {
                            null
                        }
                    }
                    if (instituteAccessServersFiltered.isNotEmpty()) {
                        resultList += OrganizationAdapter.OrganizationAdapterItem.Header(
                            R.drawable.ic_institute,
                            R.string.header_institute_access
                        )
                        resultList += instituteAccessServersFiltered
                    }
                    if (secureInternetServersFiltered.isNotEmpty()) {
                        resultList += OrganizationAdapter.OrganizationAdapterItem.Header(
                            R.drawable.ic_secure_internet,
                            R.string.header_secure_internet
                        )
                        resultList += secureInternetServersFiltered
                    }
                    resultList
                }
            }
        }
    }

    val noItemsFound = Transformations.switchMap(state) { state ->
        Transformations.map(adapterItems) { items ->
            items.isEmpty() && state == ConnectionState.Ready
        }
    }

    fun selectOrganizationAndInstance(organization: Organization?, instance: Instance) {
        preferencesService.setCurrentOrganization(organization)
        if (organization == null) {
            discoverApi(instance)
        } else {
            discoverApi(Instance(baseURI = organization.orgId, displayName = organization.displayName, authorizationType = AuthorizationType.Distributed))
        }
    }

    companion object {
        private val TAG = OrganizationSelectionViewModel::class.java.name
    }
}
