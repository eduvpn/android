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
import android.widget.Toast;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import nl.eduvpn.app.service.VPNService;

/**
 * Fragment which displays the available settings to the user.
 * Created by Daniel Zolnai on 2016-10-22.
 */
public class SettingsFragment extends BaseFragment<FragmentSettingsBinding> {

    @Inject
    protected PreferencesService _preferencesService;

    @Inject
    protected HistoryService _historyService;

    @Inject
    protected VPNService _vpnService;

    private Settings _originalSettings;

    @Override
    protected int getLayout() {
        return R.layout.fragment_settings;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EduVPNApplication.get(view.getContext()).component().inject(this);

        _originalSettings = _preferencesService.getAppSettings();

        binding.setOrganizationsEnabled(BuildConfig.NEW_ORGANIZATION_LIST_ENABLED);

        binding.useCustomTabsSwitch.setChecked(_originalSettings.useCustomTabs());
        binding.forceTcpSwitch.setChecked(_originalSettings.forceTcp());

        binding.useCustomTabsSwitch.setOnClickListener(v -> onSettingChanged());
        binding.forceTcpSwitch.setOnClickListener(v -> onSettingChanged());
        binding.saveButton.setOnClickListener(v -> onSaveButtonClicked());
        binding.licensesContainer.setOnClickListener(v -> startActivity(new Intent(requireContext(), LicenseActivity.class)));
        binding.resetAppDataContainer.setOnClickListener(v -> onResetDataClicked());
        binding.addServerContainer.setOnClickListener(v -> onAddServerClicked());
    }

    private void onAddServerClicked() {
        requireActivity().setResult(SettingsActivity.RESULT_ADD_CUSTOM_SERVER);
        requireActivity().finish();
    }

    private void onResetDataClicked() {
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
    }

    public void onSettingChanged() {
        boolean useCustomTabs = binding.useCustomTabsSwitch.isChecked();
        boolean forceTcp = binding.forceTcpSwitch.isChecked();
        boolean settingsChanged = useCustomTabs != _originalSettings.useCustomTabs() || forceTcp != _originalSettings.forceTcp();
        binding.saveButton.setEnabled(settingsChanged);
    }

    protected void onSaveButtonClicked() {
        boolean useCustomTabs = binding.useCustomTabsSwitch.isChecked();
        boolean forceTcp = binding.forceTcpSwitch.isChecked();
        _preferencesService.storeAppSettings(new Settings(useCustomTabs, forceTcp));
        Toast.makeText(getContext(), R.string.settings_saved, Toast.LENGTH_LONG).show();
        requireActivity().finish();
    }
}
