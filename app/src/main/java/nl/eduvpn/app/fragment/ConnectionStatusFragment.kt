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
package nl.eduvpn.app.fragment

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.eduvpn.app.Constants
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentConnectionStatusBinding
import nl.eduvpn.app.entity.v3.ProfileV3API
import nl.eduvpn.app.fragment.ServerSelectionFragment.Companion.newInstance
import nl.eduvpn.app.service.VPNConnectionService
import nl.eduvpn.app.service.VPNService
import nl.eduvpn.app.service.VPNService.VPNStatus
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.FormattingUtils
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.viewmodel.BaseConnectionViewModel
import nl.eduvpn.app.viewmodel.ConnectionStatusViewModel
import javax.inject.Inject

/**
 * The fragment which displays the status of the current connection.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class ConnectionStatusFragment : BaseFragment<FragmentConnectionStatusBinding>() {

    private val gracefulDisconnectHandler = Handler(Looper.getMainLooper())

    private var isAutomaticCheckChange = false
    private var skipNextDisconnect = true

    @Inject
    protected lateinit var vpnService: VPNService

    override val layout = R.layout.fragment_connection_status

    private val viewModel by viewModels<ConnectionStatusViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("XXX Created: $this")
        EduVPNApplication.get(view.context).component().inject(this)
        binding.viewModel = viewModel
        binding.secondsConnected = viewModel.connectionTimeLiveData.map { secondsConnected ->
            println("XXX received: ${this@ConnectionStatusFragment}")
            FormattingUtils.formatDurationSeconds(
                requireContext(),
                secondsConnected
            )
        }
        binding.bytesDownloaded = viewModel.byteCountFlow.map { bc ->
            FormattingUtils.formatBytesTraffic(
                requireContext(),
                bc?.bytesIn
            )
        }.asLiveData()
        binding.bytesUploaded = viewModel.byteCountFlow.map { bc ->
            FormattingUtils.formatBytesTraffic(
                requireContext(),
                bc?.bytesOut
            )
        }.asLiveData()
        binding.ips = viewModel.ipFLow.asLiveData()
        binding.connectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isAutomaticCheckChange) {
                return@setOnCheckedChangeListener
            }
            if (!isChecked) {
                disconnect()
            } else {
                // Get the config again, and connect again
                viewModel.reconnectWithCurrentProfile()
            }
        }
        binding.connectionInfoDropdown.setOnClickListener {
            val isOpen = binding.connectionInfoContainer.visibility == View.VISIBLE
            if (isOpen) {
                binding.connectionInfoContainer.visibility = View.GONE
                binding.dropdownIcon.animate().rotation(-90f).setDuration(300L).start()
            } else {
                binding.connectionInfoContainer.visibility = View.VISIBLE
                binding.dropdownIcon.animate().rotation(90f).setDuration(300L).start()
            }
        }
        binding.profileSwitcher.setOnClickListener {
            if (viewModel.isInDisconnectMode.value == true) {
                val profileItems = viewModel.serverProfiles.value ?: emptyList()
                AlertDialog.Builder(requireContext(), R.style.AppTheme_AlertDialog)
                    .setTitle(R.string.connection_select_profile)
                    .setItems(profileItems.map { it.displayName.bestTranslation }
                        .toTypedArray()) { _, which ->
                        val profileToConnectTo = profileItems[which]
                        activity?.let {
                            connectToProfile(profileToConnectTo)
                        }
                    }.show()
            } else {
                AlertDialog.Builder(requireContext(), R.style.AppTheme_AlertDialog)
                    .setTitle(R.string.connection_warning_disconnect_first_title)
                    .setMessage(R.string.connection_warning_disconnect_first_message)
                    .setPositiveButton(R.string.connection_warning_disconnect_first_ok_button) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        binding.renewSession.setOnClickListener {
            disconnect()
            viewModel.renewSession()
        }
        viewModel.connectionParentAction.observe(viewLifecycleOwner) { parentAction ->
            when (parentAction) {
                ConnectionStatusViewModel.ParentAction.SessionExpired -> {
                    val context = requireContext()
                    val dialog = ErrorDialog.show(
                        context,
                        R.string.error_certificate_expired_title,
                        R.string.error_certificate_expired_message
                    )
                    disconnect()
                    dialog?.setOnDismissListener {
                        returnToHome()
                    }
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(Constants.CERT_EXPIRY_NOTIFICATION_ID)
                }
            }
        }
        viewModel.parentAction.observe(viewLifecycleOwner) { parentAction ->
            when (parentAction) {
                is BaseConnectionViewModel.ParentAction.DisplayError -> {
                    ErrorDialog.show(requireContext(), parentAction.title, parentAction.message)
                }
                is BaseConnectionViewModel.ParentAction.OpenProfileSelector -> {
                    (activity as? MainActivity)?.openFragment(
                        ProfileSelectionFragment.newInstance(
                            parentAction.profiles
                        ), true
                    )
                }
                else -> {
                    // Do nothing.
                }
            }
        }
        val backPressedCallback =
            object : OnBackPressedCallback(viewModel.isInDisconnectMode.value ?: false) {
                override fun handleOnBackPressed() {
                    returnToHome()
                }
            }
        (activity as? MainActivity)?.onBackPressedDispatcher
            ?.addCallback(viewLifecycleOwner, backPressedCallback)
        viewModel.isInDisconnectMode.observe(viewLifecycleOwner) { isInDisconnectMode ->
            (activity as? MainActivity)?.setBackNavigationEnabled(isInDisconnectMode)
            backPressedCallback.isEnabled = isInDisconnectMode
        }
        var updateCertExpiryObserver: Observer<Unit>? = null
        updateCertExpiryObserver = Observer {
            if (!viewModel.updateCertExpiry()) {
                updateCertExpiryObserver?.let { obs -> viewModel.timer.removeObserver(obs) }
            }
        }
        viewModel.timer.observe(viewLifecycleOwner, updateCertExpiryObserver)
        val vpnStatusObserver = { vpnStatus: VPNStatus ->
            binding.connectionStatus.setText(VPNConnectionService.vpnStatusToStringID(vpnStatus))
            when (vpnStatus) {
                VPNStatus.CONNECTED -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_connected)
                    skipNextDisconnect = false
                    setToggleCheckedWithoutAction(true)
                    viewModel.isInDisconnectMode.value = false
                }
                VPNStatus.CONNECTING -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_connecting)
                    skipNextDisconnect = false
                    setToggleCheckedWithoutAction(true)
                    viewModel.isInDisconnectMode.value = false
                }
                VPNStatus.PAUSED -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_connecting)
                    skipNextDisconnect = false
                    setToggleCheckedWithoutAction(true)
                    viewModel.isInDisconnectMode.value = false
                }
                VPNStatus.DISCONNECTED -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_disconnected)
                    if (!skipNextDisconnect) {
                        // The first disconnect can mess a bit with the UI so we skip this part in special cases
                        setToggleCheckedWithoutAction(false)
                    }
                    gracefulDisconnectHandler.removeCallbacksAndMessages(null)
                    viewModel.isInDisconnectMode.value = true
                }
                VPNStatus.FAILED -> {
                    skipNextDisconnect = false
                    val message =
                        getString(R.string.error_while_connecting, vpnService.getErrorString())
                    ErrorDialog.show(
                        requireContext(),
                        R.string.error_dialog_title_unable_to_connect,
                        message
                    )
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_disconnected)
                    setToggleCheckedWithoutAction(false)
                    viewModel.isInDisconnectMode.value = true
                }
            }
        }
        // Update the icon immediately
        vpnStatusObserver(vpnService.getStatus())
        vpnService.observe(viewLifecycleOwner, vpnStatusObserver)
    }

    private fun setToggleCheckedWithoutAction(isChecked: Boolean) {
        isAutomaticCheckChange = true
        binding.connectionSwitch.isChecked = isChecked
        isAutomaticCheckChange = false
    }

    fun returnToHome() {
        disconnect()
        val activity = activity as MainActivity?
        if (activity != null && !activity.isFinishing) {
            activity.setBackNavigationEnabled(false)
            activity.openFragment(newInstance(false), false)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    fun reconnectToInstance() {
        viewModel.reconnectWithCurrentProfile()
    }

    private fun connectToProfile(profile: ProfileV3API) {
        skipNextDisconnect = true
        viewModel.isInDisconnectMode.value = false
        setToggleCheckedWithoutAction(true)
        viewModel.viewModelScope.launch {
            viewModel.selectProfileToConnectTo(profile).onFailure { thr ->
                withContext(Dispatchers.Main) {
                    setToggleCheckedWithoutAction(false)
                    viewModel.isInDisconnectMode.value = true
                    ErrorDialog.show(requireContext(), thr)
                }
            }
        }
    }

    private fun disconnect(retryCount: Int = 0) {
        val isConnecting = vpnService.getStatus() == VPNStatus.CONNECTING
        viewModel.disconnectWithCall(vpnService)
        if (isConnecting) {
            // In this case, if we call disconnect, the process can be killed.
            // That means we won't get any notification from the disconnect event.
            // So we add a timer which waits for the disconnect event. If not received, we assume the process was killed.
            gracefulDisconnectHandler.postDelayed({
                if (activity?.isFinishing != false) {
                    Log.i(TAG, "Nothing to do, already finishing activity.")
                    return@postDelayed
                }
                Log.i(TAG, "No disconnect event received from VPN within $WAIT_FOR_DISCONNECT_UNTIL_MS milliseconds. Assuming process died.")
                if (retryCount < 3) {
                    disconnect(retryCount + 1)
                } else {
                    viewModel.isInDisconnectMode.value = true
                }
            }, WAIT_FOR_DISCONNECT_UNTIL_MS.toLong())
        }
    }

    companion object {
        private const val WAIT_FOR_DISCONNECT_UNTIL_MS = 1_000
        private val TAG = ConnectionStatusFragment::class.java.name
    }
}
