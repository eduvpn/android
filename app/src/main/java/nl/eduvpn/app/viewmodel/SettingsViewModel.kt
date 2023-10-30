package nl.eduvpn.app.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import nl.eduvpn.app.entity.Settings
import nl.eduvpn.app.service.BackendService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.PreferencesService
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val historyService: HistoryService,
    private val preferencesService: PreferencesService,
    private val backendService: BackendService
) : ViewModel() {

    val appSettings get() = preferencesService.getAppSettings()

    val apiLogFile get() = backendService.getLogFile()

    val hasAddedServers get() = historyService.addedServers?.hasServers() == true

    fun removeOrganizationData() {
        historyService.removeOrganizationData()
    }

    fun storeAppSettings(appSettings: Settings) {
        preferencesService.storeAppSettings(appSettings)
    }
}