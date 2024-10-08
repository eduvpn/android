/*
 *  This file is part of eduVPN.
 *
 *     eduVPN is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     eduVPN is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.eduvpn.app.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.eduvpn.app.entity.OrganizationList
import nl.eduvpn.app.entity.ServerList

/**
 * Service which provides the configurations for organization related data model.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class OrganizationService(
    private val serializerService: SerializerService,
    private val backendService: BackendService
) {

    suspend fun fetchServerList(searchFilter: String) : Result<ServerList> = withContext(Dispatchers.IO) {
        try {
            val serverListString = backendService.discoverServers(searchFilter)
            val result = serializerService.deserializeServerList(serverListString)
            return@withContext Result.success(result)
        } catch (throwable: Throwable) {
            return@withContext Result.failure(throwable)
        }
    }

    suspend fun fetchOrganizations(searchFilter: String): OrganizationList = withContext(Dispatchers.IO) {
        val organizationListString = backendService.discoverOrganizations(searchFilter)
        serializerService.deserializeOrganizationList(organizationListString)
    }
}
