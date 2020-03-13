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

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import nl.eduvpn.app.R
import nl.eduvpn.app.base.BaseViewModel
import nl.eduvpn.app.entity.InstanceList
import nl.eduvpn.app.entity.Organization
import nl.eduvpn.app.entity.OrganizationServer
import nl.eduvpn.app.service.APIService
import nl.eduvpn.app.service.SerializerService
import org.json.JSONObject

class OrganizationSelectionViewModel(private val apiService: APIService,
                                     private val serializerService: SerializerService) : BaseViewModel() {

    sealed class ParentAction {
        data class DisplayError(@StringRes val title: Int, val message: String) : ParentAction()
        data class OpenServerSelector(val servers: List<OrganizationServer>) : ParentAction()
    }

    val state = MutableLiveData<ConnectionState>().also { it.value = ConnectionState.Ready }
    val parentAction = MutableLiveData<ParentAction>()


    fun selectOrganization(organization: Organization) {
        apiService.getJSON(organization.serverInfoUrl, null, object: APIService.Callback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val serversList = serializerService.deserializeOrganizationServerList(result)
                parentAction.postValue(ParentAction.OpenServerSelector(serversList))
            }

            override fun onError(errorMessage: String) {
                parentAction.postValue(ParentAction.DisplayError(R.string.error_fetching_organization_servers, errorMessage))
            }
        })
    }
}