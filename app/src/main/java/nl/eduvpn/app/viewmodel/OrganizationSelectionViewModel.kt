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
import nl.eduvpn.app.entity.Organization
import nl.eduvpn.app.service.OrganizationService
import nl.eduvpn.app.service.PreferencesService

class OrganizationSelectionViewModel(
        organizationService: OrganizationService,
        private val preferencesService: PreferencesService) : BaseViewModel() {

    sealed class ParentAction {
        object OpenProviderSelector : ParentAction()
        data class DisplayError(@StringRes val title: Int, val message: String) : ParentAction()
    }

    val state = MutableLiveData<ConnectionState>().also { it.value = ConnectionState.Ready }
    val parentAction = MutableLiveData<ParentAction>()

    val organizations = MutableLiveData<List<Organization>>()

    init {
        state.value = ConnectionState.FetchingOrganizations
        disposables.add(
                organizationService.fetchOrganizations()
                        .subscribe({ organizationList ->
                            state.value = ConnectionState.Ready
                            organizations.value = organizationList
                        }, { throwable ->
                            parentAction.value = ParentAction.DisplayError(R.string.error_fetching_organizations, throwable.toString())
                        })
        )
    }


    fun selectOrganization(organization: Organization) {
        preferencesService.setCurrentOrganization(organization)
        parentAction.postValue(ParentAction.OpenProviderSelector)
    }
}