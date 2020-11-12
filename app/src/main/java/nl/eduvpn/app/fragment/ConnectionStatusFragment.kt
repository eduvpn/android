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

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import de.blinkt.openvpn.VpnProfile
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.MainActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentConnectionStatusBinding
import nl.eduvpn.app.fragment.ServerSelectionFragment.Companion.newInstance
import nl.eduvpn.app.service.VPNService
import nl.eduvpn.app.service.VPNService.ConnectionInfoCallback
import nl.eduvpn.app.service.VPNService.VPNStatus
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.FormattingUtils
import nl.eduvpn.app.utils.Log
import nl.eduvpn.app.viewmodel.BaseConnectionViewModel
import nl.eduvpn.app.viewmodel.ConnectionStatusViewModel
import java.util.Observable
import java.util.Observer
import javax.inject.Inject

/**
 * The fragment which displays the status of the current connection.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class ConnectionStatusFragment : BaseFragment<FragmentConnectionStatusBinding>(), ConnectionInfoCallback {
    private var vpnStatusObserver: Observer? = null
    private val gracefulDisconnectHandler = Handler()

    private var isAutomaticCheckChange = false
    private var skipNextDisconnect = true


    @Inject
    protected lateinit var vpnService: VPNService

    override val layout = R.layout.fragment_connection_status

    private val viewModel by viewModels<ConnectionStatusViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)
        binding.viewModel = viewModel
        binding.connectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isAutomaticCheckChange) {
                return@setOnCheckedChangeListener
            }
            if (!isChecked) {
                disconnect()
            } else {
                val currentProfile = viewModel.findCurrentProfile()
                val config = viewModel.findCurrentConfig()
                if (config != null) {
                    connect(config)
                } else if (currentProfile != null) {
                    viewModel.selectProfileToConnectTo(currentProfile)
                } else {
                    // Should not happen
                    returnToHome()
                }
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
                        .setItems(profileItems.map { it.displayName }.toTypedArray()) { _, which ->
                            val profileToConnectTo = profileItems[which]
                            activity?.let {
                                viewModel.isInDisconnectMode.value = false
                                viewModel.selectProfileToConnectTo(profileToConnectTo)
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
        viewModel.connectionParentAction.observe(viewLifecycleOwner) { parentAction ->
            when (parentAction) {
                ConnectionStatusViewModel.ParentAction.SessionExpired -> {
                    val dialog = ErrorDialog.show(requireContext(), R.string.error_certificate_expired_title, R.string.error_certificate_expired_message)
                    disconnect()
                    dialog?.setOnDismissListener {
                        returnToHome()
                    }
                }
            }
        }
        viewModel.parentAction.observe(viewLifecycleOwner) { parentAction ->
            when (parentAction) {
                is BaseConnectionViewModel.ParentAction.InitiateConnection -> {
                    activity?.let { activity ->
                        if (!activity.isFinishing) {
                            viewModel.initiateConnection(activity, parentAction.instance, parentAction.discoveredAPI)
                        }
                    }
                }
                is BaseConnectionViewModel.ParentAction.ConnectWithProfile -> {
                    viewModel.refreshProfile()
                    viewModel.openVpnConnectionToProfile(requireActivity(), parentAction.vpnProfile)
                }
                is BaseConnectionViewModel.ParentAction.DisplayError -> {
                    ErrorDialog.show(requireContext(), parentAction.title, parentAction.message)
                }
            }
        }
        viewModel.isInDisconnectMode.observe(viewLifecycleOwner) { isInDisconnectMode ->
            (activity as? MainActivity)?.setBackNavigationEnabled(isInDisconnectMode)
        }
    }

    override fun onStart() {
        super.onStart()
        vpnStatusObserver = Observer { _: Observable?, arg: Any? ->
            when (arg as VPNStatus?) {
                VPNStatus.CONNECTED -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_connected)
                    binding.connectionStatus.setText(R.string.connection_info_state_connected)
                    skipNextDisconnect = false
                    isAutomaticCheckChange = true
                    binding.connectionSwitch.isChecked = true
                    isAutomaticCheckChange = false
                }
                VPNStatus.CONNECTING -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_connecting)
                    binding.connectionStatus.setText(R.string.connection_info_state_connecting)
                    skipNextDisconnect = false
                    isAutomaticCheckChange = true
                    binding.connectionSwitch.isChecked = true
                    isAutomaticCheckChange = false
                }
                VPNStatus.PAUSED -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_connecting)
                    binding.connectionStatus.setText(R.string.connection_info_state_paused)
                    skipNextDisconnect = false
                    isAutomaticCheckChange = true
                    binding.connectionSwitch.isChecked = true
                    isAutomaticCheckChange = false
                }
                VPNStatus.DISCONNECTED -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_disconnected)
                    binding.connectionStatus.setText(R.string.connection_info_state_disconnected)
                    if (!skipNextDisconnect) {
                        // The first disconnect can mess a bit with the UI so we skip this part in special cases
                        isAutomaticCheckChange = true
                        binding.connectionSwitch.isChecked = false
                        isAutomaticCheckChange = false
                    }
                    gracefulDisconnectHandler.removeCallbacksAndMessages(null)
                }
                VPNStatus.FAILED -> {
                    skipNextDisconnect = false
                    val message = getString(R.string.error_while_connecting, vpnService.errorString)
                    ErrorDialog.show(requireContext(), R.string.error_dialog_title_unable_to_connect, message)
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_disconnected)
                    binding.connectionStatus.setText(R.string.connection_info_state_disconnected)
                    isAutomaticCheckChange = true
                    binding.connectionSwitch.isChecked = false
                    isAutomaticCheckChange = false
                }
                else -> throw RuntimeException("Unhandled VPN status!")
            }
        }
        // Update the icon immediately
        vpnStatusObserver?.update(vpnService, vpnService.status)
        vpnService.addObserver(vpnStatusObserver)
        vpnService.attachConnectionInfoListener(this)
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

    override fun onStop() {
        super.onStop()
        if (vpnStatusObserver != null) {
            vpnService.deleteObserver(vpnStatusObserver)
        }
        vpnService.detachConnectionInfoListener()
    }

    private fun connect(vpnProfile: VpnProfile) {
        skipNextDisconnect = true
        vpnService.connect(requireActivity(), vpnProfile)
        viewModel.isInDisconnectMode.value = false
    }

    private fun disconnect(retryCount: Int = 0) {
        val isConnecting = vpnService.status == VPNStatus.CONNECTING
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
                    vpnService.disconnect()
                    viewModel.isInDisconnectMode.value = true
                }
            }, WAIT_FOR_DISCONNECT_UNTIL_MS.toLong())
        } else {
            vpnService.disconnect()
            viewModel.isInDisconnectMode.value = true
        }
    }

    override fun updateStatus(secondsConnected: Long?, bytesIn: Long?, bytesOut: Long?) {
        binding.valueDuration.text = FormattingUtils.formatDurationSeconds(context, secondsConnected)
        binding.valueDowloaded.text = FormattingUtils.formatBytesTraffic(context, bytesIn)
        binding.valueUploaded.text = FormattingUtils.formatBytesTraffic(context, bytesOut)
    }

    override fun metadataAvailable(localIpV4: String?, localIpV6: String?) {
        val ipV4DisplayText = localIpV4 ?: getString(R.string.not_available)
        binding.valueIpv4.text = ipV4DisplayText
        val ipV6DisplayText = localIpV6 ?: getString(R.string.not_available)
        binding.valueIpv6.text = ipV6DisplayText
    }

    companion object {
        private const val WAIT_FOR_DISCONNECT_UNTIL_MS = 1_000
        private val TAG = ConnectionStatusFragment::class.java.name
    }
}