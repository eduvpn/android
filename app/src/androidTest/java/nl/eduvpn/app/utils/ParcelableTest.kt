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

package nl.eduvpn.app.utils

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.eduvpn.app.entity.TranslatableString
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParcelableTest {

    @Test
    fun testTranslatableStringParcelable() {
        val expected = TranslatableString(
            mapOf("en" to "Hello", "es" to "Hola", "nl" to "Hallo")
        )
        val parcel = Parcel.obtain()
        expected.writeToParcel(parcel, 0)
        // Reset parcel for reading
        parcel.setDataPosition(0)
        val actual = TranslatableString.CREATOR.createFromParcel(parcel)
        parcel.recycle()
        Assert.assertEquals(expected, actual)
    }
}
