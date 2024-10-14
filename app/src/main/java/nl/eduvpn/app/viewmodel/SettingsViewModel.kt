package nl.eduvpn.app.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.eduvpn.app.service.BackendService
import nl.eduvpn.app.service.HistoryService
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val historyService: HistoryService,
    private val backendService: BackendService
) : ViewModel() {

    val apiLogFile get() = backendService.getLogFile()

    val hasAddedServers get() = historyService.addedServers?.hasServers() == true

    fun removeOrganizationData() {
        viewModelScope.launch(Dispatchers.IO) {
            historyService.removeOrganizationData()
        }
    }
}