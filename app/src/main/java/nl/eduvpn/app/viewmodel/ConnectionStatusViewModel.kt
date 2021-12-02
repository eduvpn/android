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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import nl.eduvpn.app.CertExpiredBroadcastReceiver
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.*
import nl.eduvpn.app.service.*
import nl.eduvpn.app.utils.getCountryText
import nl.eduvpn.app.utils.toSingleEvent
import javax.inject.Inject
import javax.inject.Named

class ConnectionStatusViewModel @Inject constructor(
    private val context: Context,
    private val preferencesService: PreferencesService,
    private val eduVPNOpenVPNService: EduVPNOpenVPNService,
    private val vpnService: VPNService,
    private val historyService: HistoryService,
    @Named("timer")
    val timer: LiveData<Unit>,
    @Named("connectionTimeLiveData")
    val connectionTimeLiveData: LiveData<Long?>,
    wireGuardService: WireGuardService,
    apiService: APIService,
    serializerService: SerializerService,
    connectionService: ConnectionService,
    vpnConnectionService: VPNConnectionService,
) : BaseConnectionViewModel(
    context, apiService, serializerService, historyService,
    preferencesService, connectionService, eduVPNOpenVPNService, wireGuardService,
    vpnConnectionService,
) {

    sealed class ParentAction {
        object SessionExpired : ParentAction()
    }

    private var certExpiryTime: Long? = null

    val serverName = MutableLiveData<String>()
    val serverSupport = MutableLiveData<String?>()
    val certValidity = MutableLiveData<Spanned?>()
    val profileName = MutableLiveData<String>()

    //todo: does not work when disconnecting outside the app
    val isInDisconnectMode = MutableLiveData(false)
    val serverProfiles = MutableLiveData<List<Profile>>()
    val byteCountLiveData = vpnService.byteCountLiveData
    val ipLiveData = vpnService.ipLiveData

    private val _connectionParentAction = MutableLiveData<ParentAction>()
    val connectionParentAction = _connectionParentAction.toSingleEvent()

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val intent = Intent(context, CertExpiredBroadcastReceiver::class.java)
        .setAction(CertExpiredBroadcastReceiver.ACTION)
    private val pendingIntent =
        PendingIntent.getBroadcast(context, 0, intent, 0)

    init {
        refreshProfile()
        val connectionInstance = preferencesService.getCurrentInstance()
        serverProfiles.value = preferencesService.getCurrentProfileList()
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
    }

    override fun onResume() {
        super.onResume()
        cancelAllExpiryNotifications()
    }

    fun onPause() {
        planExpiryNotification()
    }

    private fun planExpiryNotification() {
        val certExpiryTime = this.certExpiryTime
        if (certExpiryTime != null && vpnService.getStatus() != VPNService.VPNStatus.DISCONNECTED) {
            val maxMillisecondsBeforeNotification: Long = 30 * 60 * 1000L
            val now = System.currentTimeMillis()
            val millisecondsUntilExpiry = now - certExpiryTime
            val startMilliseconds = maxOf(certExpiryTime - maxMillisecondsBeforeNotification, now)
            val notificationWindowLength = minOf(millisecondsUntilExpiry, 15 * 60 * 1000L)
            alarmManager.setWindow(
                AlarmManager.RTC,
                startMilliseconds,
                notificationWindowLength,
                pendingIntent
            )
        }
    }

    private fun cancelAllExpiryNotifications() {
        alarmManager.cancel(pendingIntent)
    }

    fun renewSession() {
        discoverApi(preferencesService.getCurrentInstance()!!, null, true)
    }

    fun reconnectToInstance() {
        historyService.removeSavedKeyPairs(preferencesService.getCurrentInstance())
        discoverApi(preferencesService.getCurrentInstance()!!)
    }

    /**
     * @return If the cert expiry time should continue to be updated.
     */
    fun updateCertExpiry(): Boolean {
        val currentTime = System.currentTimeMillis()
        val certExpiryTime = this.certExpiryTime
        if (certExpiryTime == null) {
            // No cert or time, nothing to display
            certValidity.value = null
            return false
        }
        val timeDifferenceInSeconds = (certExpiryTime - currentTime) / 1000
        if (timeDifferenceInSeconds < 0) {
            // Cert expired
            certValidity.value = HtmlCompat.fromHtml(context.getString(R.string.connection_certificate_status_expired), HtmlCompat.FROM_HTML_MODE_COMPACT)
            _connectionParentAction.value = ParentAction.SessionExpired
            return false
        }
        if (timeDifferenceInSeconds < 60) {
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
        return true
    }

    fun findCurrentConfig(): VPNConfig? {
        val currentProfile = preferencesService.getCurrentProfile() ?: return null
        return when (currentProfile) {
            is WireGuardProfileV3 -> currentProfile.config?.let { c -> VPNConfig.WireGuard(c) }
            else -> {
                val matchingSavedProfile = preferencesService.getSavedProfileList()
                    ?.firstOrNull { it.profile.profileId == currentProfile.profileId }
                    ?: return null
                val vpnProfile = eduVPNOpenVPNService.findMatchingVpnProfile(matchingSavedProfile)
                vpnProfile?.let { p -> VPNConfig.OpenVPN(p) }
            }
        }
    }

    fun findCurrentProfile(): Profile? {
        return preferencesService.getCurrentProfile()
    }

    fun refreshProfile() {
        val savedProfile = preferencesService.getCurrentProfile()
        val connectionInstance = preferencesService.getCurrentInstance()
        if (connectionInstance?.countryCode != null) {
            serverName.value = connectionInstance.getCountryText()
        } else if (savedProfile != null) {
            serverName.value = savedProfile.displayName
        } else {
            serverName.value = context.getString(R.string.profile_name_not_found)
        }
        profileName.value = savedProfile?.displayName
        certExpiryTime = when (savedProfile) {
            is ProfileV2 -> historyService.getSavedKeyPairForInstance(connectionInstance).keyPair.expiryTimeMillis
            is ProfileV3 -> savedProfile.expiry
            null -> null
        }
        updateCertExpiry()
    }

    companion object {
        private val TAG = ConnectionStatusViewModel::class.qualifiedName
    }
}
