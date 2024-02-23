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

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import de.blinkt.openvpn.activities.LogWindow
import nl.eduvpn.app.ApiLogsActivity
import nl.eduvpn.app.BuildConfig
import nl.eduvpn.app.EduVPNApplication
import nl.eduvpn.app.LicenseActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.SettingsActivity
import nl.eduvpn.app.base.BaseFragment
import nl.eduvpn.app.databinding.FragmentSettingsBinding
import nl.eduvpn.app.entity.Settings
import nl.eduvpn.app.viewmodel.SettingsViewModel

/**
 * Fragment which displays the available settings to the user.
 * Created by Daniel Zolnai on 2016-10-22.
 */
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    val viewModel by viewModels<SettingsViewModel>{ viewModelFactory }

    override val layout = R.layout.fragment_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EduVPNApplication.get(view.context).component().inject(this)
        val originalSettings = viewModel.appSettings
        binding.useCustomTabsSwitch.isChecked = originalSettings.useCustomTabs()
        binding.preferTcpSwitch.isChecked = originalSettings.preferTcp()
        binding.useCustomTabsSwitch.setOnClickListener { saveSettings() }
        binding.preferTcpSwitch.setOnClickListener { saveSettings() }
        binding.licensesButton.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    LicenseActivity::class.java
                )
            )
        }
        binding.resetDataButton.setOnClickListener { onResetDataClicked() }
        binding.viewOpenvpnLogsButton.setOnClickListener {
            val intent = Intent(activity, LogWindow::class.java)
            startActivity(intent)
        }
        binding.viewApiLogsButton.setOnClickListener {
            val intent = Intent(activity, ApiLogsActivity::class.java)
            startActivity(intent)
        }
        binding.viewApiLogsContainer.isVisible = viewModel.apiLogFile != null
        if (!BuildConfig.API_DISCOVERY_ENABLED) {
            binding.resetDataSeparator.visibility = View.GONE
            binding.resetAppDataContainer.visibility = View.GONE
        }
    }

    private fun onResetDataClicked() {
        if (viewModel.hasAddedServers) {
            val resetDataDialog = AlertDialog.Builder(requireContext())
                .setTitle(R.string.reset_data_dialog_title)
                .setMessage(R.string.reset_data_dialog_message)
                .setPositiveButton(R.string.reset_data_dialog_yes) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    try {
                        viewModel.removeOrganizationData()
                    } catch (ex: Exception) {
                        AlertDialog.Builder(requireContext())
                            .setTitle(R.string.unexpected_error)
                            .setMessage(ex.message)
                            .setPositiveButton(R.string.go_back) { secondDialog, _ -> secondDialog.dismiss() }
                            .show()
                    }
                    requireActivity().setResult(SettingsActivity.RESULT_APP_DATA_CLEARED)
                    requireActivity().finish()
                }
                .setNegativeButton(R.string.reset_data_dialog_no) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .create()
            resetDataDialog.show()
        } else {
            val warningDialog = AlertDialog.Builder(requireContext())
                .setTitle(R.string.warning_no_organization_title)
                .setMessage(R.string.warning_no_organization_message)
                .setPositiveButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .create()
            warningDialog.show()
        }
    }

    private fun saveSettings() {
        val useCustomTabs = binding.useCustomTabsSwitch.isChecked
        val preferTcp = binding.preferTcpSwitch.isChecked
        viewModel.storeAppSettings(Settings(useCustomTabs, preferTcp))
    }
}