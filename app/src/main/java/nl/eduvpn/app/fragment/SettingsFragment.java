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

package nl.eduvpn.app.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import javax.inject.Inject;

import de.blinkt.openvpn.activities.LogWindow;
import nl.eduvpn.app.BuildConfig;
import nl.eduvpn.app.EduVPNApplication;
import nl.eduvpn.app.LicenseActivity;
import nl.eduvpn.app.R;
import nl.eduvpn.app.SettingsActivity;
import nl.eduvpn.app.base.BaseFragment;
import nl.eduvpn.app.databinding.FragmentSettingsBinding;
import nl.eduvpn.app.entity.Settings;
import nl.eduvpn.app.service.HistoryService;
import nl.eduvpn.app.service.PreferencesService;

/**
 * Fragment which displays the available settings to the user.
 * Created by Daniel Zolnai on 2016-10-22.
 */
public class SettingsFragment extends BaseFragment<FragmentSettingsBinding> {

    @Inject
    protected PreferencesService _preferencesService;

    @Inject
    protected HistoryService _historyService;

    @Override
    protected int getLayout() {
        return R.layout.fragment_settings;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EduVPNApplication.get(view.getContext()).component().inject(this);

        Settings _originalSettings = _preferencesService.getAppSettings();

        binding.useCustomTabsSwitch.setChecked(_originalSettings.useCustomTabs());
        binding.forceTcpSwitch.setChecked(_originalSettings.forceTcp());

        binding.useCustomTabsSwitch.setOnClickListener(v -> saveSettings());
        binding.forceTcpSwitch.setOnClickListener(v -> saveSettings());
        binding.licensesButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), LicenseActivity.class)));
        binding.resetDataButton.setOnClickListener(v -> onResetDataClicked());
        binding.viewLogButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LogWindow.class);
            startActivity(intent);
        });

        if (!BuildConfig.API_DISCOVERY_ENABLED) {
            binding.resetDataSeparator.setVisibility(View.GONE);
            binding.resetAppDataContainer.setVisibility(View.GONE);
        }

    }

    private void onResetDataClicked() {
        if (_historyService.getOrganizationList() != null) {
            AlertDialog resetDataDialog = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.reset_data_dialog_title)
                    .setMessage(R.string.reset_data_dialog_message)
                    .setPositiveButton(R.string.reset_data_dialog_yes, (dialog, which) -> {
                        dialog.dismiss();
                        _historyService.removeOrganizationData();
                        requireActivity().setResult(SettingsActivity.RESULT_APP_DATA_CLEARED);
                        requireActivity().finish();
                    })
                    .setNegativeButton(R.string.reset_data_dialog_no, (dialog, which) -> dialog.dismiss()).create();
            resetDataDialog.show();
        } else {
            AlertDialog warningDialog = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.warning_no_organization_title)
                    .setMessage(R.string.warning_no_organization_message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .create();
            warningDialog.show();
        }
    }

    protected void saveSettings() {
        boolean useCustomTabs = binding.useCustomTabsSwitch.isChecked();
        boolean forceTcp = binding.forceTcpSwitch.isChecked();
        _preferencesService.storeAppSettings(new Settings(useCustomTabs, forceTcp));
    }
}
