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
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
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
    private var userInitiatedDisconnect = false
    private var userNavigation = false
    private val gracefulDisconnectHandler = Handler()

    @Inject
    protected lateinit var vpnService: VPNService

    override val layout = R.layout.fragment_connection_status

    private val viewModel by viewModels<ConnectionStatusViewModel> { viewModelFactory }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)
        binding.viewModel = viewModel
        binding.connectionSwitch.setOnClickListener {
            val isChecked = binding.connectionSwitch.isChecked
            if (!isChecked) {
                onDisconnectButtonClicked()
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
        viewModel.parentAction.observe(viewLifecycleOwner) { parentAction ->
            when (parentAction) {
                ConnectionStatusViewModel.ParentAction.SessionExpired -> {
                    ErrorDialog.show(requireContext(), R.string.error_certificate_expired_title, R.string.error_certificate_expired_message)
                    onDisconnectButtonClicked()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vpnStatusObserver = Observer { _: Observable?, arg: Any? ->
            when (arg as VPNStatus?) {
                VPNStatus.CONNECTED -> {
                    userNavigation = false
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_connected)
                    binding.connectionStatus.setText(R.string.connection_info_state_connected)
                    binding.connectionSwitch.isChecked = true
                }
                VPNStatus.CONNECTING -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_connecting)
                    binding.connectionStatus.setText(R.string.connection_info_state_connecting)
                    userNavigation = false
                    binding.connectionSwitch.isEnabled = true
                    binding.connectionSwitch.isChecked = true
                }
                VPNStatus.PAUSED -> {
                    binding.connectionStatus.setText(R.string.connection_info_state_paused)
                    binding.connectionSwitch.isEnabled = true
                    binding.connectionSwitch.isChecked = true
                    userNavigation = false
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_connecting)
                }
                VPNStatus.DISCONNECTED -> if (userInitiatedDisconnect) {
                    // Go back to the home screen.
                    binding.connectionSwitch.isEnabled = false
                    userNavigation = false
                    gracefulDisconnectHandler.removeCallbacksAndMessages(null)
                    returnToHome()
                } else {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_disconnected)
                    binding.connectionStatus.setText(R.string.connection_info_state_disconnected)
                    binding.connectionSwitch.isEnabled = true
                    binding.connectionSwitch.isChecked = false
                    userNavigation = true
                }
                VPNStatus.FAILED -> {
                    val message = getString(R.string.error_while_connecting, vpnService.errorString)
                    ErrorDialog.show(requireContext(), R.string.error_dialog_title_unable_to_connect, message)
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_disconnected)
                    binding.connectionStatus.setText(R.string.connection_info_state_disconnected)
                }
                else -> throw RuntimeException("Unhandled VPN status!")
            }
        }
        // Update the icon immediately
        vpnStatusObserver?.update(vpnService, vpnService.status)
        vpnService.addObserver(vpnStatusObserver)
        vpnService.attachConnectionInfoListener(this)
    }

    private fun returnToHome() {
        val activity = activity as MainActivity?
        if (activity != null && !activity.isFinishing) {
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

    private fun onDisconnectButtonClicked() {
        if (userNavigation) {
            returnToHome()
        } else {
            val isConnecting = vpnService.status == VPNStatus.CONNECTING
            userInitiatedDisconnect = true
            binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_disconnected)
            binding.connectionSwitch.isEnabled = false
            vpnService.disconnect()
            if (isConnecting) {
                // In this case, if we call disconnect, the process can be killed.
                // That means we won't get any notification from the disconnect event.
                // So we add a timer which waits for the disconnect event. If not received, we assume the process was killed.
                gracefulDisconnectHandler.postDelayed({
                    if (activity?.isFinishing != false) {
                        Log.i(TAG, "Cannot close connection status fragment, because activity was already finished. User probably left the app.")
                        return@postDelayed
                    }
                    Log.i(TAG, "No disconnect event received from VPN within $WAIT_FOR_DISCONNECT_UNTIL_MS milliseconds. Assuming process died.")
                    returnToHome()
                }, WAIT_FOR_DISCONNECT_UNTIL_MS.toLong())
            }
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
        private const val WAIT_FOR_DISCONNECT_UNTIL_MS = 3000
        private val TAG = ConnectionStatusFragment::class.java.name
    }
}