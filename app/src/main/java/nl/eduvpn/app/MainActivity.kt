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
package nl.eduvpn.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.eduvpn.app.base.BaseActivity
import nl.eduvpn.app.databinding.ActivityMainBinding
import nl.eduvpn.app.entity.exception.CommonException
import nl.eduvpn.app.fragment.AddServerFragment
import nl.eduvpn.app.fragment.ConnectionStatusFragment
import nl.eduvpn.app.fragment.OrganizationSelectionFragment
import nl.eduvpn.app.fragment.ProfileSelectionFragment
import nl.eduvpn.app.fragment.ServerSelectionFragment
import nl.eduvpn.app.service.BackendService
import nl.eduvpn.app.service.EduVPNOpenVPNService
import nl.eduvpn.app.service.HistoryService
import nl.eduvpn.app.service.VPNService
import nl.eduvpn.app.utils.ErrorDialog.show
import nl.eduvpn.app.viewmodel.MainViewModel
import nl.eduvpn.app.viewmodel.ViewModelFactory
import java.util.*
import javax.inject.Inject


class MainActivity : BaseActivity<ActivityMainBinding>() {

    companion object {
        private const val REQUEST_CODE_SETTINGS = 1001
        private const val KEY_BACK_NAVIGATION_ENABLED = "back_navigation_enabled"
    }

    @Inject
    protected lateinit var vpnService: Optional<VPNService>

    @Inject
    protected lateinit var eduVPNOpenVPNService: EduVPNOpenVPNService

    @Inject
    protected lateinit var viewModelFactory : ViewModelFactory

    protected val viewModel by viewModels<MainViewModel> { viewModelFactory }

    private var _backNavigationEnabled = false
    private var _parseIntentOnStart = true

    override val layout: Int = R.layout.activity_main

    private fun createCertExpiryNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.cert_expiry_channel_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channelID = Constants.CERT_EXPIRY_NOTIFICATION_CHANNEL_ID
            val channel = NotificationChannel(channelID, name, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createVPNConnectionNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.vpn_connection_channel_name)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channelID = Constants.VPN_CONNECTION_NOTIFICATION_CHANNEL_ID
            val channel = NotificationChannel(channelID, name, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EduVPNApplication.get(this).component().inject(this)
        setSupportActionBar(binding.toolbar.toolbar)
        viewModel.mainParentAction.observe(this) { parentAction ->
            when (parentAction) {
                is MainViewModel.MainParentAction.OpenLink -> {
                    openLink(parentAction.oAuthUrl)
                }
                is MainViewModel.MainParentAction.SelectCountry -> {
                    selectCountry(parentAction.cookie)
                }
                is MainViewModel.MainParentAction.SelectProfiles -> {
                    openFragment(ProfileSelectionFragment.newInstance(parentAction.profileList), false)
                }
                is MainViewModel.MainParentAction.ConnectWithConfig -> {
                    viewModel.parseConfigAndStartConnection(this, parentAction.config)
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.content_frame)
                    if (currentFragment !is ConnectionStatusFragment) {
                        openFragment(ConnectionStatusFragment(), false)
                    }
                }
                is MainViewModel.MainParentAction.ShowError -> {
                    show(this, parentAction.throwable)
                }
            }
        }
        eduVPNOpenVPNService.onCreate(this)
        if (savedInstanceState == null) {
            // If there's an ongoing VPN connection, open the status screen.
            if (vpnService.isPresent  && vpnService.get().getStatus() != VPNService.VPNStatus.DISCONNECTED) {
                openFragment(ConnectionStatusFragment(), false)
            } else if (viewModel.hasServers()) {
                openFragment(ServerSelectionFragment.newInstance(false), false)
            } else if (BuildConfig.API_DISCOVERY_ENABLED) {
                openFragment(OrganizationSelectionFragment(), false)
            } else {
                openFragment(AddServerFragment(), false)
            }
        } else if (savedInstanceState.containsKey(KEY_BACK_NAVIGATION_ENABLED)) {
            _backNavigationEnabled = savedInstanceState.getBoolean(KEY_BACK_NAVIGATION_ENABLED)
        }
        _parseIntentOnStart = true
        binding.toolbar.settingsButton.setOnClickListener { _: View? -> onSettingsButtonClicked() }
        binding.toolbar.helpButton.setOnClickListener { _: View? ->
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Constants.HELP_URI
                )
            )
        }
        createCertExpiryNotificationChannel()
        createVPNConnectionNotificationChannel()
    }

    fun selectCountry(cookie: Int? = null) {
        viewModel.getCountryList(this, cookie)
    }

    private fun openLink(oAuthUrl: String) {
        if (isFinishing) {
            return
        }
        val oAuthUri: Uri
        try {
            oAuthUri = Uri.parse(oAuthUrl)
        } catch (ex: Exception) {
            show(this, ex)
            return
        }
        if (viewModel.useCustomTabs()) {
            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(this@MainActivity, oAuthUri)
        } else {
            val intent = Intent(Intent.ACTION_VIEW, oAuthUri)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onStart() {
        super.onStart()
        if (_parseIntentOnStart) {
            // The app might have been reopened from a URL.
            _parseIntentOnStart = false
            onNewIntent(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_BACK_NAVIGATION_ENABLED, _backNavigationEnabled)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (viewModel.handleRedirection(intent.data)) {
                    // Remove it so we don't parse it again.
                    intent.data = null
                    val currentFragment =
                        supportFragmentManager.findFragmentById(R.id.content_frame)
                    withContext(Dispatchers.Main) {
                        when (currentFragment) {
                            is ServerSelectionFragment -> currentFragment.connectToSelectedInstance()
                            is OrganizationSelectionFragment -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    R.string.provider_added_new_configs_available,
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            is ConnectionStatusFragment -> currentFragment.reconnectToInstance()
                        }
                    }
                    if (currentFragment !is ConnectionStatusFragment
                        && currentFragment !is ServerSelectionFragment
                    ) {
                        openFragment(ServerSelectionFragment.newInstance(true), false)
                    }
                }
            } catch (ex: CommonException) {
                withContext(Dispatchers.Main) {
                    show(
                        this@MainActivity, R.string.authorization_error_title, getString(
                            R.string.authorization_error_message,
                            ex.javaClass.simpleName,
                            0,
                            ex.message
                        )
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        eduVPNOpenVPNService.onDestroy(this)
    }

    fun openFragment(fragment: Fragment?, openOnTop: Boolean) {
        if (openOnTop) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .addToBackStack(null)
                .add(R.id.content_frame, fragment!!)
                .commitAllowingStateLoss()
        } else {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.content_frame, fragment!!)
                .commitAllowingStateLoss()
            // Clean the back stack
            for (i in 0 until supportFragmentManager.backStackEntryCount) {
                if (!supportFragmentManager.isStateSaved && !isFinishing) {
                    supportFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun onSettingsButtonClicked() {
        val intent = Intent(this, SettingsActivity::class.java)
        @Suppress("DEPRECATION") //todo
        startActivityForResult(intent, REQUEST_CODE_SETTINGS)
    }

    fun setBackNavigationEnabled(enabled: Boolean) {
        _backNavigationEnabled = enabled
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(enabled)
            actionBar.setHomeButtonEnabled(enabled)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SETTINGS) {
            if (resultCode == SettingsActivity.RESULT_APP_DATA_CLEARED) {
                if (vpnService.isPresent
                    && vpnService.get().getStatus() != VPNService.VPNStatus.DISCONNECTED
                ) {
                    vpnService.get().disconnect()
                }
                openFragment(OrganizationSelectionFragment(), false)
            }
        } else {
            @Suppress("DEPRECATION")
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
