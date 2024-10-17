package nl.eduvpn.app

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import de.blinkt.openvpn.fragments.LogFragment
import nl.eduvpn.app.base.BaseActivity
import nl.eduvpn.app.databinding.ActivityApiLogsBinding
import nl.eduvpn.app.databinding.ActivityOpenvpnLogsBinding
import nl.eduvpn.app.viewmodel.ApiLogsViewModel
import nl.eduvpn.app.viewmodel.ViewModelFactory
import javax.inject.Inject

class OpenVpnLogsActivity : BaseActivity<ActivityOpenvpnLogsBinding>() {

    override val layout = R.layout.activity_openvpn_logs

    @Inject
    protected lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EduVPNApplication.get(this).component().inject(this)
        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.settingsButton.isVisible = false
        supportFragmentManager.beginTransaction()
            .add(binding.fragmentContainer.id, LogFragment())
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}