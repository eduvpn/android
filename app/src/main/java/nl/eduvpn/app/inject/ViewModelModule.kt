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

package nl.eduvpn.app.inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import nl.eduvpn.app.viewmodel.*

@Module
interface ViewModelModule {

    @Binds
    fun bindViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(OrganizationSelectionViewModel::class)
    fun bindOrganizationSelectionViewModel(organizationSelectionViewModel: OrganizationSelectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProviderSelectionViewModel::class)
    fun bindProviderSelectionViewModel(providerSelectionViewModel: ProviderSelectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ServerSelectionViewModel::class)
    fun bindServerSelectionViewModel(serverSelectionViewModel: ServerSelectionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConnectionStatusViewModel::class)
    fun bindConnectionStatusViewModel(connectionStatusViewModel: ConnectionStatusViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileSelectionViewModel::class)
    fun bindProfileSelectionViewModel(profileSelectionViewModel: ProfileSelectionViewModel) : ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddServerViewModel::class)
    fun bindAddServerViewModel(addServerViewModel: AddServerViewModel) : ViewModel

}