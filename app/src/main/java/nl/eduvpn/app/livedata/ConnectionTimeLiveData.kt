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

package nl.eduvpn.app.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import nl.eduvpn.app.service.VPNService

/**
 * Amount of seconds connected to the VPN.
 */
object ConnectionTimeLiveData {
    
    fun create(
        vpnStatusLiveData: LiveData<VPNService.VPNStatus>,
        timer: LiveData<Unit>
    ): LiveData<Long?> {
        var connectionTime = 0L

        val connectionTimeLiveData = MediatorLiveData<Long?>()

        val update = {
            connectionTimeLiveData.value = (System.currentTimeMillis() - connectionTime) / 1000L
        }

        // we do not want to miss connected / disconnected events when the user puts the app in the
        // background, so observeForever
        vpnStatusLiveData.observeForever { vpnStatus ->
            if (vpnStatus == VPNService.VPNStatus.CONNECTED) {
                connectionTime = System.currentTimeMillis()
                connectionTimeLiveData.value = 0
                try {
                    // the timer will be automatically stopped onPause and will be restarted onResume
                    connectionTimeLiveData.addSource(timer) { update() }
                } catch (ex: IllegalArgumentException) {
                    // timer already added as source
                }
            } else if (vpnStatus == VPNService.VPNStatus.DISCONNECTED) {
                connectionTimeLiveData.removeSource(timer)
                connectionTimeLiveData.value = null
            }
        }

        return connectionTimeLiveData
    }
}
