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
import de.blinkt.openvpn.core.VpnStatus

class ByteCountLiveData : LiveData<ByteCountLiveData.ByteCount>() {

    data class ByteCount(val bytesIn: Long, val bytesOut: Long)

    private val byteCountListener: VpnStatus.ByteCountListener =
        VpnStatus.ByteCountListener { inBytes, outBytes, _, _ ->
            postValue(ByteCount(inBytes, outBytes))
        }

    override fun onActive() {
        VpnStatus.addByteCountListener(byteCountListener)
    }

    override fun onInactive() {
        VpnStatus.removeByteCountListener(byteCountListener)
    }
}
