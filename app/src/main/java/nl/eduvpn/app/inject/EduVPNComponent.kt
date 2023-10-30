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
package nl.eduvpn.app.inject

import dagger.Component
import nl.eduvpn.app.ApiLogsActivity
import nl.eduvpn.app.CertExpiredBroadcastReceiver
import nl.eduvpn.app.DisconnectVPNBroadcastReceiver
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.fragment.*
import javax.inject.Singleton

/**
 * The Dagger component which executes the injections.
 * Created by Daniel Zolnai on 2016-10-07.
 */
@Singleton
@Component(modules = [ApplicationModule::class])
interface EduVPNComponent {
    object Initializer {
        @JvmStatic
        fun init(application: EduVPNApplication?): EduVPNComponent { // Don't worry if you see an error here, DaggerEduVPNComponent is generated while building.
            return DaggerEduVPNComponent.builder()
                .applicationModule(ApplicationModule(application!!)).build()
        }
    }
  
    fun inject(organizationSelectionFragment: OrganizationSelectionFragment)
    fun inject(mainActivity: MainActivity)
    fun inject(apiLogsActivity: ApiLogsActivity)
    fun inject(connectionStatusFragment: ConnectionStatusFragment)
    fun inject(homeFragment: ProfileSelectionFragment)
    fun inject(settingsFragment: SettingsFragment)
    fun inject(serverSelectionFragment: ServerSelectionFragment)
    fun inject(addServerFragment: AddServerFragment)
    fun inject(certExpiredBroadcastReceiver: CertExpiredBroadcastReceiver)
    fun inject(disconnectVPNBroadcastReceiver: DisconnectVPNBroadcastReceiver)
}
