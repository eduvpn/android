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
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.postDelayed
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
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
import nl.eduvpn.app.entity.Profile
import nl.eduvpn.app.fragment.ServerSelectionFragment.Companion.newInstance
import nl.eduvpn.app.service.VPNConnectionService
import nl.eduvpn.app.service.VPNService.VPNStatus
import nl.eduvpn.app.utils.ErrorDialog
import nl.eduvpn.app.utils.FormattingUtils
import nl.eduvpn.app.viewmodel.BaseConnectionViewModel
import nl.eduvpn.app.viewmodel.ConnectionStatusViewModel
import nl.eduvpn.app.viewmodel.MainViewModel
import org.eduvpn.common.Protocol

/**
 * The fragment which displays the status of the current connection.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class ConnectionStatusFragment : BaseFragment<FragmentConnectionStatusBinding>() {

    private var isAutomaticCheckChange = false
    private var skipNextDisconnect = true

    override val layout = R.layout.fragment_connection_status

    private val viewModel by viewModels<ConnectionStatusViewModel> { viewModelFactory }

    private val mainViewModel by activityViewModels<MainViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)
        binding.viewModel = viewModel
        binding.isTcp = viewModel.isCurrentProtocolUsingTcp()
        binding.failoverNeeded = mainViewModel.failoverResult.value ?: false
        binding.secondsConnected = viewModel.connectionTimeLiveData.map { secondsConnected ->
            val context = this@ConnectionStatusFragment.context ?: return@map null
            FormattingUtils.formatDurationSeconds(
                context,
                secondsConnected
            )
        }
        binding.bytesDownloaded = viewModel.byteCountFlow.map { bc ->
            val context = this@ConnectionStatusFragment.context ?: return@map null
            FormattingUtils.formatBytesTraffic(
                context,
                bc?.bytesIn
            )
        }.asLiveData()
        binding.bytesUploaded = viewModel.byteCountFlow.map { bc ->
            val context = this@ConnectionStatusFragment.context ?: return@map null
            FormattingUtils.formatBytesTraffic(
                context,
                bc?.bytesOut
            )
        }.asLiveData()
        binding.protocolName = getProtocolName(viewModel.protocol)
        binding.ips = viewModel.ipFLow.asLiveData()
        binding.connectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isAutomaticCheckChange) {
                return@setOnCheckedChangeListener
            }
            if (!isChecked) {
                viewModel.disconnect(activity)
            } else {
                // Get the config again, and connect again
                viewModel.reconnectWithCurrentProfile(viewModel.isCurrentProtocolUsingTcp())
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
            viewModel.disconnect(activity)
            viewModel.renewSession()
        }
        binding.reconnectTcpButton.setOnClickListener { button ->
            activity?.let {
                viewModel.disconnect(it)
            }
            button.postDelayed(100) {
                viewModel.enableTcp()
                binding.isTcp = true
                viewModel.reconnectWithCurrentProfile(preferTcp = true)
            }
        }
        viewModel.connectionParentAction.observe(viewLifecycleOwner) { parentAction ->
            when (parentAction) {
                ConnectionStatusViewModel.ParentAction.SessionExpired -> {
                    val activity = activity ?: return@observe
                    val dialog = ErrorDialog.show(
                        activity,
                        R.string.error_certificate_expired_title,
                        R.string.error_certificate_expired_message
                    )
                    viewModel.disconnect(activity)
                    dialog?.listener = object : ErrorDialog.ErrorDialogFragment.Listener {
                        override fun onDismiss() {
                            returnToHome()
                        }
                    }
                    val notificationManager =
                        activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(Constants.CERT_EXPIRY_NOTIFICATION_ID)
                }
            }
        }
        viewModel.parentAction.observe(viewLifecycleOwner) { parentAction ->
            when (parentAction) {
                is BaseConnectionViewModel.ParentAction.DisplayError -> {
                    ErrorDialog.show(requireActivity(), parentAction.title, parentAction.message)
                }
                is BaseConnectionViewModel.ParentAction.ShowContextCanceledToast -> {
                    Snackbar.make(view, parentAction.message, Snackbar.LENGTH_LONG).show()
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
        mainViewModel.failoverResult.observe(viewLifecycleOwner) {
            binding.failoverNeeded = it
        }
        viewModel.timer.observe(viewLifecycleOwner, updateCertExpiryObserver)
        viewModel.vpnStatus.observe(viewLifecycleOwner) { status ->
            binding.connectionStatus.setText(VPNConnectionService.vpnStatusToStringID(status))
            when (status) {
                VPNStatus.CONNECTED -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_connected)
                    skipNextDisconnect = false
                    setToggleCheckedWithoutAction(true)
                }
                VPNStatus.CONNECTING -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_connecting)
                    skipNextDisconnect = false
                    setToggleCheckedWithoutAction(true)
                }
                VPNStatus.PAUSED -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_connecting)
                    skipNextDisconnect = false
                    setToggleCheckedWithoutAction(true)
                }
                VPNStatus.DISCONNECTED -> {
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_disconnected)
                    if (!skipNextDisconnect) {
                        // The first disconnect can mess a bit with the UI so we skip this part in special cases
                        setToggleCheckedWithoutAction(false)
                    }
                }
                VPNStatus.FAILED -> {
                    skipNextDisconnect = false
                    val message =
                        getString(R.string.error_while_connecting, viewModel.getVpnErrorString())
                    ErrorDialog.show(
                        requireActivity(),
                        R.string.error_dialog_title_unable_to_connect,
                        message
                    )
                    binding.connectionStatusIcon.setImageResource(R.drawable.ic_connection_status_disconnected)
                    setToggleCheckedWithoutAction(false)
                }
            }
        }
    }

    private fun getProtocolName(protocol: Protocol): String? {
        return when(protocol) {
            Protocol.OpenVPN -> getString(R.string.connection_info_protocol_name_openvpn)
            Protocol.WireGuard -> getString(R.string.connection_info_protocol_name_wireguard)
            Protocol.WireGuardWithTCP -> getString(R.string.connection_info_protocol_name_wireguard_with_tcp)
            Protocol.OpenVPNWithTCP -> getString(R.string.connection_info_protocol_name_openvpn_with_tcp)
            Protocol.Unknown -> null
        }
    }

    private fun setToggleCheckedWithoutAction(isChecked: Boolean) {
        isAutomaticCheckChange = true
        binding.connectionSwitch.isChecked = isChecked
        isAutomaticCheckChange = false
    }

    fun returnToHome() {
        viewModel.disconnect(activity)
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
        viewModel.reconnectWithCurrentProfile(viewModel.isCurrentProtocolUsingTcp())
    }

    private fun connectToProfile(profile: Profile) {
        skipNextDisconnect = true
        viewModel.isInDisconnectMode.value = false
        setToggleCheckedWithoutAction(true)
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            viewModel.selectProfileToConnectTo(profile, preferTcp = false).onFailure { thr ->
                withContext(Dispatchers.Main) {
                    setToggleCheckedWithoutAction(false)
                    viewModel.isInDisconnectMode.value = true
                    ErrorDialog.show(requireActivity(), thr)
                }
            }.onSuccess {
                // Relaunch this fragment. This is required, because in some cases, the backend
                // implementation (WireGuard vs OpenVPN) might be different, and we would be connected
                // to the incorrect one.
                (activity as? MainActivity)?.openFragment(ConnectionStatusFragment(), openOnTop = false)
            }
        }
    }
}
