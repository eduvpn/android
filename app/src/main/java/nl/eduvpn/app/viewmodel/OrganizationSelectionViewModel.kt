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
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
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
    vpnConnectionService: VPNConnectionService,
) : BaseConnectionViewModel(
    context,
    backendService,
    historyService,
    preferencesService,
    vpnConnectionService,
) {
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
                val organizationListDeferred = if (!historyService.hasSecureInternetServer()) {
                    connectionState.postValue(ConnectionState.FetchingOrganizations)
                    async {
                        val organizationList = organizationService.fetchOrganizations()
                        historyService.organizationList = organizationList
                        organizationList
                    }
                } else {
                    // We can't show any organization servers (secure internet), user needs to reset to switch.
                    connectionState.postValue(ConnectionState.FetchingServerList)
                    CompletableDeferred(OrganizationList(emptyList()))
                }
                val cachedServerList = preferencesService.getServerList()
                val serverListDeferred = if (cachedServerList != null) {
                    CompletableDeferred(cachedServerList)
                } else {
                    async { organizationService.fetchServerList() }
                }

                val organizationList =
                    runCatchingCoroutine { organizationListDeferred.await() }.getOrElse {
                        Log.w(TAG, "Organizations call has failed!", it)
                        OrganizationList(emptyList())
                    }

                val serverList = runCatchingCoroutine { serverListDeferred.await() }.getOrElse {
                    Log.w(TAG, "Server list call has failed!", it)
                    ServerList(emptyList())
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
                connectionState.postValue(ConnectionState.Ready)
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

    val adapterItems = organizations.switchMap { organizations ->
        instituteAccessServers.switchMap { instituteAccessServers ->
            secureInternetServers.switchMap { secureInternetServers ->
                searchText.map { searchText ->
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
                    val secureInternetServersFiltered = if (historyService.hasSecureInternetServer()) {
                        secureInternetServers.filter {
                            matchesServer(searchText, it.displayName, it.keywords)
                        }.map {
                            OrganizationAdapter.OrganizationAdapterItem.SecureInternet(it)
                        }
                    } else {
                        organizations.filter {
                            matchesServer(searchText, it.displayName, it.keywordList)
                        }.map {
                            OrganizationAdapter.OrganizationAdapterItem.Organization(it)
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

    val noItemsFound = connectionState.switchMap { state ->
        adapterItems.map { items ->
            items.isEmpty() && state == ConnectionState.Ready
        }
    }

    companion object {
        private val TAG = OrganizationSelectionViewModel::class.java.name
    }
}
