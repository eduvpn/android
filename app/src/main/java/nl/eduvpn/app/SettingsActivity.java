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

package nl.eduvpn.app;

import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import nl.eduvpn.app.base.BaseActivity;
import nl.eduvpn.app.databinding.ActivitySettingsBinding;
import nl.eduvpn.app.fragment.SettingsFragment;

/**
 * Activity which displays the settings fragment.
 * Created by Daniel Zolnai on 2016-10-22.
 */
public class SettingsActivity extends BaseActivity<ActivitySettingsBinding> {

    public static final int RESULT_APP_DATA_CLEARED = 101;

    @Override
    protected int getLayout() {
        return R.layout.activity_settings;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentFrame, new SettingsFragment())
                .commit();
        binding.toolbar.settingsButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.mainColor)));
    }
}
