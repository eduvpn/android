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

package nl.eduvpn.app.entity

import android.os.Parcel
import android.os.Parcelable
import android.os.PersistableBundle
import android.text.TextUtils
import kotlinx.serialization.Serializable
import nl.eduvpn.app.Constants
import nl.eduvpn.app.utils.serializer.TranslatableStringSerializer
import java.util.*

@Serializable(with = TranslatableStringSerializer::class)
data class TranslatableString(
    val translations: Map<String, String>
) : Parcelable {
    constructor(defaultValue: String) : this(mapOf("en" to defaultValue))
    constructor() : this(emptyMap())


    /***
     * Finds the best matching translation.
     * Strings with matching language (and locale) take precedence.
     *
     * @return The best match from the object. Could be null if there are no translations at all.
     ***/
    val bestTranslation: String?
        get() {
            val entrySet = translations.entries
            var matchingLevel = 0
            var bestTranslationMatch: String? = null
            // 0 - no matching
            // 1 - matches any item (will be the first item, if no better match)
            // 2 - item in english language
            // 3 - language part matches, territory does not
            // 4 - language part matches, territory part matches, variant does not
            // 5 - full match
            for (entry in entrySet) {
                val key = entry.key
                val localeParts = key.split("-".toRegex()).toTypedArray()
                var translationLocale: Locale
                translationLocale = if (localeParts.size == 1) {
                    Locale(localeParts[0])
                } else if (localeParts.size == 2) {
                    Locale(localeParts[0], localeParts[1])
                } else {
                    val variant = TextUtils.join("-", localeParts.copyOfRange(2, localeParts.size))
                    Locale(localeParts[0], localeParts[1], variant)
                }
                var currentMatchingLevel = 1
                if (translationLocale.language.equals(Constants.LOCALE.language, ignoreCase = true)) {
                    currentMatchingLevel = 3
                    if (translationLocale.country.equals(Constants.LOCALE.country, ignoreCase = true)) {
                        currentMatchingLevel = 4
                        if (translationLocale.variant.equals(Constants.LOCALE.variant, ignoreCase = true)) {
                            currentMatchingLevel = 5
                        }
                    }
                } else if (translationLocale.language.equals("en", ignoreCase = true)) {
                    currentMatchingLevel = 2
                }
                if (currentMatchingLevel > matchingLevel) {
                    matchingLevel = currentMatchingLevel
                    bestTranslationMatch = entry.value
                }
                if (currentMatchingLevel == 5) {
                    break
                }
            }
            return bestTranslationMatch
        }

    override fun writeToParcel(out: Parcel, flags: Int) {
        val persistableBundle = PersistableBundle(translations.size)
        translations.forEach { (k, v) ->
            persistableBundle.putString(k, v)
        }
        out.writePersistableBundle(persistableBundle)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TranslatableString> {
        override fun createFromParcel(inParcel: Parcel): TranslatableString {
            val bundle = inParcel.readPersistableBundle(this::class.java.classLoader)!!
            val map = bundle.keySet().map { k -> Pair(k, bundle.getString(k)!!) }.toMap()
            return TranslatableString(map)
        }

        override fun newArray(size: Int): Array<TranslatableString?> {
            return arrayOfNulls(size)
        }
    }

}
