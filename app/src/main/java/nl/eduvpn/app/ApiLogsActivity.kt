package nl.eduvpn.app

import android.os.Bundle
import androidx.activity.viewModels
import nl.eduvpn.app.base.BaseActivity
import nl.eduvpn.app.databinding.ActivityApiLogsBinding
import nl.eduvpn.app.viewmodel.ApiLogsViewModel
import nl.eduvpn.app.viewmodel.ViewModelFactory
import javax.inject.Inject

class ApiLogsActivity : BaseActivity<ActivityApiLogsBinding>() {

    override val layout = R.layout.activity_api_logs

    @Inject
    protected lateinit var viewModelFactory: ViewModelFactory

    private val viewModel by viewModels<ApiLogsViewModel> { viewModelFactory }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EduVPNApplication.get(this).component().inject(this)
    }

    override fun onResume() {
        super.onResume()
        binding.logContents.text = viewModel.getLogFileContents()
    }
}