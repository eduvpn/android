/*
 * This file is part of eduVPN.
 *
 * eduVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * eduVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package nl.eduvpn.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.view.isVisible
import nl.eduvpn.app.base.BaseActivity
import nl.eduvpn.app.databinding.ActivityLicensesBinding

class LicenseActivity : BaseActivity<ActivityLicensesBinding>() {

    override val layout = R.layout.activity_licenses

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.webView.loadUrl("file:///android_asset/licenses.html");
        binding.toolbar.settingsButton.isVisible = false
        binding.toolbar.helpButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Constants.HELP_URI))
        }
    }

}