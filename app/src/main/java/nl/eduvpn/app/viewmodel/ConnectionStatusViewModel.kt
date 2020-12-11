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
import android.os.Handler
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.lifecycle.MutableLiveData
import de.blinkt.openvpn.VpnProfile
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.service.*
import nl.eduvpn.app.utils.getCountryText
import nl.eduvpn.app.utils.toSingleEvent
import javax.inject.Inject

class ConnectionStatusViewModel @Inject constructor(
        private val context: Context,
        private val preferencesService: PreferencesService,
        private val vpnService: VPNService,
        historyService: HistoryService,
        apiService: APIService,
        serializerService: SerializerService,
        connectionService: ConnectionService
) : BaseConnectionViewModel(context, apiService, serializerService, historyService,
        preferencesService, connectionService, vpnService) {

    sealed class ParentAction {
        object SessionExpired : ParentAction()
    }

    private val certExpiryTime: Long?

    val serverName = MutableLiveData<String>()
    val serverSupport = MutableLiveData<String>()
    val certValidity = MutableLiveData<Spanned>()
    val profileName = MutableLiveData<String>()
    val isInDisconnectMode = MutableLiveData(false)
    val serverProfiles = MutableLiveData<List<Profile>>()

    private val _connectionParentAction = MutableLiveData<ParentAction>()
    val connectionParentAction = _connectionParentAction.toSingleEvent()

    private var updateCertHandler: Handler = Handler()
    private var updateCertCallback: Runnable = Runnable {
        updateCertExpiry()
        runUpdateCertExpiryEverySecond()
    }


    init {
        refreshProfile()
        val connectionInstance = preferencesService.currentInstance
        serverProfiles.value = preferencesService.currentProfileList
        if (connectionInstance != null && connectionInstance.supportContact.isNotEmpty()) {
            val supportContacts = StringBuilder()
            for (contact in connectionInstance.supportContact) {
                if (contact.startsWith("mailto:")) {
                    supportContacts.append(contact.replace("mailto:", ""))
                } else if (contact.startsWith("tel:")) {
                    supportContacts.append(contact.replace("tel:", ""))
                } else {
                    supportContacts.append(contact)
                }
                supportContacts.append(", ")
            }
            // Remove the last separator
            supportContacts.delete(supportContacts.length - 2, supportContacts.length)
            serverSupport.value = context.getString(R.string.connection_info_support, supportContacts)
        } else {
            serverSupport.value = null
        }
        certExpiryTime = historyService.getSavedKeyPairForInstance(connectionInstance).keyPair.expiryTimeMillis
    }

    override fun onResume() {
        super.onResume()
        runUpdateCertExpiryEverySecond()
    }

    fun onPause() {
        stopUpdateCertExpiry()
    }

    private fun runUpdateCertExpiryEverySecond() {
        updateCertHandler.postDelayed(updateCertCallback, 1 * 1000)
    }

    private fun stopUpdateCertExpiry() {
        updateCertHandler.removeCallbacks(updateCertCallback)
    }

    private fun updateCertExpiry() {
        val currentTime = System.currentTimeMillis()
        if (certExpiryTime == null) {
            // No cert or time, nothing to display
            stopUpdateCertExpiry()
            certValidity.value = null
            return
        }
        val timeDifferenceInSeconds = (certExpiryTime - currentTime) / 1000
        if (timeDifferenceInSeconds < 0) {
            // Cert expired
            stopUpdateCertExpiry()
            certValidity.value = HtmlCompat.fromHtml(context.getString(R.string.connection_certificate_status_expired), HtmlCompat.FROM_HTML_MODE_COMPACT)
            _connectionParentAction.value = ParentAction.SessionExpired
        } else if (timeDifferenceInSeconds < 60) {
            // Expires within a minute
            val seconds = context.resources.getQuantityString(R.plurals.certificate_status_seconds, timeDifferenceInSeconds.toInt(), timeDifferenceInSeconds)
            certValidity.value = HtmlCompat.fromHtml(context.getString(R.string.connection_certificate_status_valid_for_one_part, seconds), HtmlCompat.FROM_HTML_MODE_COMPACT)
        } else if (timeDifferenceInSeconds < 3600) {
            // Expires within an hour
            val seconds = context.resources.getQuantityString(R.plurals.certificate_status_seconds, timeDifferenceInSeconds.rem(60).toInt(), timeDifferenceInSeconds.rem(60).toInt())
            val minutes = context.resources.getQuantityString(R.plurals.certificate_status_minutes, timeDifferenceInSeconds.div(60).toInt(), timeDifferenceInSeconds.div(60).toInt())
            certValidity.value = HtmlCompat.fromHtml(context.getString(R.string.connection_certificate_status_valid_for_two_parts, minutes, seconds), HtmlCompat.FROM_HTML_MODE_COMPACT)
        } else if (timeDifferenceInSeconds < 3600 * 24) {
            // Expires within a day
            val hours = context.resources.getQuantityString(R.plurals.certificate_status_hours, timeDifferenceInSeconds.div(3600).toInt(), timeDifferenceInSeconds.div(3600).toInt())
            val minutes = context.resources.getQuantityString(R.plurals.certificate_status_minutes, timeDifferenceInSeconds.rem(3600).div(60).toInt(), timeDifferenceInSeconds.rem(3600).div(60).toInt())
            certValidity.value = HtmlCompat.fromHtml(context.getString(R.string.connection_certificate_status_valid_for_two_parts, hours, minutes), HtmlCompat.FROM_HTML_MODE_COMPACT)
        } else if (timeDifferenceInSeconds < 3600 * 24 * 30) {
            // Expires within 30 days
            val days = context.resources.getQuantityString(R.plurals.certificate_status_days, timeDifferenceInSeconds.div(3600 * 24).toInt(), timeDifferenceInSeconds.div(3600 * 24).toInt())
            val hours = context.resources.getQuantityString(R.plurals.certificate_status_hours, timeDifferenceInSeconds.rem(3600 * 24).div(3600).toInt(), timeDifferenceInSeconds.rem(3600 * 24).div(3600).toInt())
            certValidity.value = HtmlCompat.fromHtml(context.getString(R.string.connection_certificate_status_valid_for_two_parts, days, hours), HtmlCompat.FROM_HTML_MODE_COMPACT)
        } else {
            // More than 30 days
            val days = context.resources.getQuantityString(R.plurals.certificate_status_days, timeDifferenceInSeconds.div(3600 * 24).toInt(), timeDifferenceInSeconds.div(3600 * 24).toInt())
            certValidity.value = HtmlCompat.fromHtml(context.getString(R.string.connection_certificate_status_valid_for_one_part, days), HtmlCompat.FROM_HTML_MODE_COMPACT)
        }
    }

    fun findCurrentConfig(): VpnProfile? {
        val currentProfile = preferencesService.currentProfile ?: return null
        val matchingSavedProfile = preferencesService.savedProfileList?.firstOrNull { it.profile.profileId == currentProfile.profileId }
                ?: return null
        return vpnService.findMatchingVpnProfile(matchingSavedProfile)
    }

    fun findCurrentProfile(): Profile? {
        return preferencesService.currentProfile
    }

    fun refreshProfile() {
        val savedProfile = preferencesService.currentProfile
        val connectionInstance = preferencesService.currentInstance
        if (connectionInstance?.countryCode != null) {
            serverName.value = connectionInstance.getCountryText()
        } else if (savedProfile != null) {
            serverName.value = savedProfile.displayName
        } else {
            serverName.value = context.getString(R.string.profile_name_not_found)
        }
        profileName.value = savedProfile?.displayName
        updateCertExpiry()
    }

    companion object {
        private val TAG = ConnectionStatusViewModel::class.qualifiedName
    }
}
