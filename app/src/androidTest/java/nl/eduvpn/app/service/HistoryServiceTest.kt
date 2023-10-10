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
package nl.eduvpn.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import nl.eduvpn.app.entity.*
import org.junit.*
import org.junit.runner.RunWith
import java.util.*

/**
 * Tests for the history service.
 * Created by Daniel Zolnai on 2016-10-22.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HistoryServiceTest {

    private var _historyService: HistoryService? = null

    @Before
    @After
    @Throws(Exception::class)
    fun clearPrefs() {
        reloadHistoryService(true)
    }

    /**
     * Reloads the service, so we can test if the (de)serialization works as expected.
     *
     * @param clearHistory If the history should be cleared beforehand,
     */
    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    private fun reloadHistoryService(clearHistory: Boolean) {
        val serializerService = SerializerService()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferencesService = PreferencesService(context, serializerService)
        // Clean the shared preferences if needed
        if (clearHistory) {
            preferencesService.clearPreferences()
        } else {
            // By doing a new commit, we make sure that all other pending transactions are being taken care of
            preferencesService.getSharedPreferences()
                .edit()
                .putInt("DUMMY_KEY", Random().nextInt())
                .commit()
        }
        _historyService = HistoryService(BackendService(context, serializerService, preferencesService))
    }
}
