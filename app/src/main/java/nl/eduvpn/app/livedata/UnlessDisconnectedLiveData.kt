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
 * Live data unless VPN disconnected.
 */
object UnlessDisconnectedLiveData {
    fun <T> create(
        liveData: LiveData<T>,
        vpnStatusLiveData: LiveData<VPNService.VPNStatus>
    ): LiveData<T?> {
        val mediator = MediatorLiveData<T?>()
        mediator.addSource(liveData) { value ->
            mediator.value = if (vpnStatusLiveData.value == VPNService.VPNStatus.DISCONNECTED) {
                null
            } else {
                value
            }
        }
        mediator.addSource(vpnStatusLiveData) { vpnStatus ->
            if (vpnStatus == VPNService.VPNStatus.DISCONNECTED) {
                mediator.value = null
            }
        }
        return mediator
    }
}
