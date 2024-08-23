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

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.eduvpn.app.CertExpiredBroadcastReceiver
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.CertExpiryTimes
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.fragment.ConnectionStatusFragment
import nl.eduvpn.app.service.*
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.pendingIntentImmutableFlag
import nl.eduvpn.app.utils.toSingleEvent
import org.eduvpn.common.Protocol
import javax.inject.Inject
import javax.inject.Named

class ConnectionStatusViewModel @Inject constructor(
    private val context: Context,
    private val preferencesService: PreferencesService,
    private val vpnService: VPNService,
    private val historyService: HistoryService,
    @Named("timer")
    val timer: LiveData<Unit>,
    @Named("connectionTimeLiveData")
    val connectionTimeLiveData: LiveData<Long?>,
    private val backendService: BackendService,
    vpnConnectionService: VPNConnectionService,
) : BaseConnectionViewModel(
    context,
    backendService,
    historyService,
    preferencesService,
    vpnConnectionService,
) {

    sealed class ParentAction {
        object SessionExpired : ParentAction()
    }

    private var certExpiryTimes: CertExpiryTimes? = null

    val serverName = MutableLiveData<String>()
    val serverSupport = MutableLiveData<String?>()
    val certValidity = MutableLiveData<Spanned?>()
    val profileName = MutableLiveData<String>()

    val isInDisconnectMode = MutableLiveData(false)
    val serverProfiles = MutableLiveData<List<Profile>>()
    val byteCountFlow = vpnService.byteCountFlow
    val ipFLow = vpnService.ipFlow
    val protocol: Protocol = if (preferencesService.getCurrentProtocol() == Protocol.WireGuardWithProxyGuard.nativeValue)  {
        Protocol.WireGuardWithProxyGuard
    } else {
        vpnService.getProtocol()
    }
    val canRenew = MutableLiveData(false)
    val vpnStatus = MutableLiveData(VPNService.VPNStatus.DISCONNECTED)

    private val _connectionParentAction = MutableLiveData<ParentAction>()
    val connectionParentAction = _connectionParentAction.toSingleEvent()

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val intent = Intent(context, CertExpiredBroadcastReceiver::class.java)
        .setAction(CertExpiredBroadcastReceiver.ACTION)
    private val pendingIntent =
        PendingIntent.getBroadcast(context, 0, intent, pendingIntentImmutableFlag)
    private val gracefulDisconnectHandler = Handler(Looper.getMainLooper())


    init {
        val currentServer = historyService.currentServer
        certExpiryTimes = historyService.certExpiryTimes
        val connectionInstance = currentServer.asInstance()
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
                serverSupport.postValue(context.getString(R.string.connection_info_support, supportContacts))
            } else {
                serverSupport.postValue(null)
            }

        val buttonTime = certExpiryTimes?.buttonTime
        if (buttonTime == null) {
            canRenew.postValue(true)
        } else {
            val now = System.currentTimeMillis() / 1000
            val remaining = buttonTime - now
            if (remaining > 0) {
                viewModelScope.launch(Dispatchers.IO) {
                    delay(remaining)
                    canRenew.postValue(true)
                }
            } else {
                canRenew.postValue(true)
            }
        }
        serverProfiles.value = currentServer.getProfiles()
        profileName.value = currentServer.currentProfile?.displayName?.bestTranslation
        serverName.value = currentServer.getDisplayName()?.bestTranslation
        viewModelScope.launch {
            vpnService.asFlow().collect { status ->
                val previousStatus = vpnStatus.value ?: VPNService.VPNStatus.DISCONNECTED
                vpnStatus.postValue(status)
                when (status) {
                    VPNService.VPNStatus.CONNECTED -> {
                        isInDisconnectMode.value = false
                        backendService.notifyConnected()
                    }
                    VPNService.VPNStatus.CONNECTING -> {
                        isInDisconnectMode.value = false
                        backendService.notifyConnecting()
                    }
                    VPNService.VPNStatus.PAUSED -> {
                        isInDisconnectMode.value = false
                    }
                    VPNService.VPNStatus.DISCONNECTED, VPNService.VPNStatus.FAILED -> {
                        gracefulDisconnectHandler.removeCallbacksAndMessages(null)
                        isInDisconnectMode.value = true
                        if (previousStatus != VPNService.VPNStatus.DISCONNECTED) {
                            backendService.notifyDisconnecting()
                            backendService.notifyDisconnected()
                            viewModelScope.launch {
                                backendService.cleanUp()
                            }
                        }
                    }
                }
            }
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
        val certExpiryTimes = this.certExpiryTimes ?: return
        val endTime = certExpiryTimes.endTime?.times(1000) ?: return // Multiplying with 1000 to convert seconds to milliseconds
        val timeNow = System.currentTimeMillis() / 1000
        val notificationTime = certExpiryTimes.notificationTimes.firstOrNull { it > timeNow }?.times(1000) // seconds to ms
        if (notificationTime != null && vpnService.getStatus() != VPNService.VPNStatus.DISCONNECTED) {
            val notificationWindowLength = minOf(endTime - System.currentTimeMillis(), 15 * 60 * 1000L)
            alarmManager.setWindow(
                AlarmManager.RTC,
                notificationTime,
                notificationWindowLength,
                pendingIntent
            )
        }
    }

    private fun cancelAllExpiryNotifications() {
        alarmManager.cancel(pendingIntent)
    }

    fun renewSession() {
        discoverApi(preferencesService.getCurrentInstance()!!)
    }

    fun reconnectWithCurrentProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentServer = backendService.getCurrentServer()?.asInstance()
            if (currentServer == null) {
                _parentAction.postValue(BaseConnectionViewModel.ParentAction.DisplayError(R.string.error_fetching_profile, context.getString(R.string.error_no_profiles_from_server)))
                return@launch
            }
            getProfiles(currentServer)
        }
    }

    /**
     * @return If the cert expiry time should continue to be updated.
     */
    fun updateCertExpiry(): Boolean {
        val currentTime = System.currentTimeMillis() / 1000
        val certExpiryTime = this.certExpiryTimes?.endTime
        if (certExpiryTime == null) {
            // No cert or time, nothing to display
            certValidity.value = null
            return false
        }
        val timeDifferenceInSeconds = (certExpiryTime - currentTime)
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

    fun disconnect(activity: Activity?, retryCount: Int = 0) {
        val isConnecting = vpnService.getStatus() == VPNService.VPNStatus.CONNECTING
        disconnectWithCall(vpnService)
        if (isConnecting) {
            // In this case, if we call disconnect, the process can be killed.
            // That means we won't get any notification from the disconnect event.
            // So we add a timer which waits for the disconnect event. If not received, we assume the process was killed.
            gracefulDisconnectHandler.postDelayed({
                if (activity?.isFinishing != false) {
                    Log.i(TAG, "Nothing to do, already finishing activity.")
                    return@postDelayed
                }
                Log.i(TAG, "No disconnect event received from VPN within ${WAIT_FOR_DISCONNECT_UNTIL_MS} milliseconds. Assuming process died.")
                if (retryCount < 3) {
                    disconnect(activity, retryCount + 1)
                } else {
                    isInDisconnectMode.value = true
                }
            }, WAIT_FOR_DISCONNECT_UNTIL_MS.toLong())
        }
    }

    fun getVpnErrorString(): String? {
        return vpnService.getErrorString()
    }

    companion object {
        private const val WAIT_FOR_DISCONNECT_UNTIL_MS = 1_000
        private val TAG = ConnectionStatusFragment::class.java.name
    }
}
