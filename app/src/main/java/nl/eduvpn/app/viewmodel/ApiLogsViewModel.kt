package nl.eduvpn.app.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import nl.eduvpn.app.service.BackendService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.PreferencesService
import javax.inject.Inject

class ApiLogsViewModel @Inject constructor(
    private val backendService: BackendService
) : ViewModel() {
    fun getLogFileContents() : String {
        return backendService.getLogFile()!!.readLines().joinToString("\n")
    }
}