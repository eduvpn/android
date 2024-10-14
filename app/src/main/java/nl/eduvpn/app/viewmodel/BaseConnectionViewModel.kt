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
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.*
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.entity.exception.CommonException
import nl.eduvpn.app.livedata.toSingleEvent
import nl.eduvpn.app.service.*
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.utils.runCatchingCoroutine
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * This viewmodel takes care of the entire flow, from connecting to the servers to fetching profiles.
 */
abstract class BaseConnectionViewModel(
    private val context: Context,
    private val backendService: BackendService,
    private val historyService: HistoryService,
    private val preferencesService: PreferencesService,
    private val vpnConnectionService: VPNConnectionService,
) : ViewModel() {

    sealed class ParentAction {
        data class DisplayError(@StringRes val title: Int, val message: String) : ParentAction()
        data class ShowContextCanceledToast(val message: String) : ParentAction()
        data class OpenProfileSelector(val profiles: List<Profile>) : ParentAction()
    }

    val connectionState = MutableLiveData(ConnectionState.Ready)

    val warning = MutableLiveData<String>()

    protected val _parentAction = MutableLiveData<ParentAction?>()
    val parentAction = _parentAction.toSingleEvent()

    fun discoverApi(instance: Instance) {
        // If no discovered API, fetch it first, then initiate the connection for the login
        connectionState.value = ConnectionState.DiscoveringApi
        // Discover the API
        viewModelScope.launch(Dispatchers.IO) {
            runCatchingCoroutine {
                preferencesService.setCurrentInstance(instance)
                backendService.addServer(instance)
            }.onSuccess {
                getProfiles(instance, preferTcp = false)
            }.onFailure { throwable ->
                Log.e(TAG, "Error while fetching discovered API.", throwable)
                connectionState.postValue(ConnectionState.Ready)
                val errorString = if (throwable is CommonException) {
                    throwable.translatedMessage()
                } else {
                    throwable.toString()
                }
                if ((throwable as? CommonException)?.isMiscError() == true) {
                    // Ignore
                    return@launch
                } else if ((throwable as? CommonException)?.isMiscError() != true) {
                    _parentAction.postValue(ParentAction.ShowContextCanceledToast(errorString))
                    return@launch
                }
                _parentAction.postValue(ParentAction.DisplayError(
                    R.string.error_dialog_title,
                    context.getString(
                        R.string.error_discover_api,
                        instance.sanitizedBaseURI,
                        errorString
                    )
                ))
            }
        }
    }

    fun getProfiles(instance: Instance, preferTcp: Boolean) {
        connectionState.postValue(ConnectionState.FetchingProfiles)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                preferencesService.setCurrentInstance(instance)
                backendService.getConfig(instance, preferTcp = preferTcp)
            } catch (ex: Exception) {
                connectionState.postValue(ConnectionState.Ready)
                val errorString = if (ex is CommonException) {
                    ex.translatedMessage()
                } else {
                    ex.toString()
                }
                if ((ex as? CommonException)?.isMiscError() == true) {
                    // Ignore
                    return@launch
                } else if ((ex as? CommonException)?.isMiscError() != true) {
                    _parentAction.postValue(ParentAction.ShowContextCanceledToast(errorString))
                    return@launch
                }

                _parentAction.postValue(ParentAction.DisplayError(
                    R.string.error_dialog_title,
                    context.getString(
                        R.string.error_fetching_profile,
                        errorString
                    )
                ))
            }
        }
    }

    @Throws(Exception::class)
    suspend fun selectProfileToConnectTo(profile: Profile, preferTcp: Boolean) : Result<Unit> {
        backendService.selectProfile(profile, preferTcp = preferTcp)
        return Result.success(Unit)
    }

    open fun onResume() {
        if (connectionState.value == ConnectionState.Authorizing) {
            connectionState.value = ConnectionState.Ready
        }
    }

    private fun <T> showError(thr: Throwable?, resourceId: Int): Result<T> {
        val message = context.getString(resourceId, thr)
        Log.e(TAG, message, thr)
        connectionState.value = ConnectionState.Ready
        _parentAction.value = ParentAction.DisplayError(
            R.string.error_dialog_title,
            message
        )
        return Result.failure(thr ?: RuntimeException(message))
    }

    fun disconnectWithCall(vpnService: VPNService) {
        vpnConnectionService.disconnect(context, vpnService)
    }

    fun deleteAllDataForInstance(instance: Instance) {
        viewModelScope.launch(Dispatchers.IO) {
            historyService.removeAllDataForInstance(instance)
        }
    }

    fun getProfileInstance(): Instance {
        return preferencesService.getCurrentInstance()!!
    }

    companion object {
        private val TAG = BaseConnectionViewModel::class.java.name
    }

}
