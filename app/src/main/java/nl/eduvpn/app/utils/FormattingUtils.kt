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

import android.content.Context
import nl.eduvpn.app.Constants
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.Instance
import java.net.URI
import java.text.DecimalFormat
import java.util.*

/**
 * Utility methods for different formatting cases.
 * Created by Daniel Zolnai on 2016-10-18.
 */
object FormattingUtils {

    private val DECIMAL_FORMAT = DecimalFormat("0.00")

    private const val BYTES_IN_A_KB: Long = 1024
    private const val BYTES_IN_A_MB = BYTES_IN_A_KB * 1024
    private const val BYTES_IN_A_GB = BYTES_IN_A_MB * 1024

    /**
     * Formats the output which displays that for how long the VPN was connected.
     *
     * @param context The application or activity context.
     * @param seconds The seconds which have elapsed since the connection was initialized. Use null for the not connected status.
     * @return The string to display in the UI.
     */
    @JvmStatic
    fun formatDurationSeconds(context: Context, seconds: Long?): String {
        return if (seconds == null) {
            context.getString(R.string.not_available)
        } else if (seconds >= 3600) {
            val hours = seconds / 3600
            val minutes = seconds / 60 % 60
            val secondsInMinute = seconds % 60
            context.getString(
                R.string.duration_more_than_one_hour,
                hours,
                minutes,
                secondsInMinute
            )
        } else {
            val minutes = seconds / 60 % 60
            val secondsInMinute = seconds % 60
            context.getString(
                R.string.duration_less_than_one_hour,
                minutes,
                secondsInMinute
            )
        }
    }

    /**
     * Formats the bytes traffic to a more readable format.
     *
     * @param context The application or activity context.
     * @param bytes   The number of bytes.
     * @return The human readable format.
     */
    @JvmStatic
    fun formatBytesTraffic(context: Context, bytes: Long?): String {
        return if (bytes == null) {
            context.getString(R.string.not_available)
        } else if (bytes < BYTES_IN_A_MB) {
            val kiloBytes = bytes.toDouble() / BYTES_IN_A_KB
            val kiloByteString = DECIMAL_FORMAT.format(kiloBytes)
            context.getString(R.string.traffic_kilobytes, kiloByteString)
        } else if (bytes < BYTES_IN_A_GB) {
            val megaBytes = bytes.toDouble() / BYTES_IN_A_MB
            val megaByteString = DECIMAL_FORMAT.format(megaBytes)
            context.getString(R.string.traffic_megabytes, megaByteString)
        } else {
            val gigaBytes = bytes.toDouble() / BYTES_IN_A_GB
            val gigaByteString = DECIMAL_FORMAT.format(gigaBytes)
            context.getString(R.string.traffic_gigabytes, gigaByteString)
        }
    }

    /**
     * Creates a name to display in the list of saved profiles.
     *
     * @param context  The application or activity context.
     * @param instance The provider which gives the first part of the name
     * @param profile  The profile ID which gives the second part of the name.
     * @return The name to display.
     */
    fun formatProfileName(context: Context, instance: Instance, profileId: String?): String {
        val instanceName = formatDisplayName(instance)
        if (profileId == null) {
            return instanceName ?: "Default"
        }
        return context.getString(
            R.string.saved_profile_display_name,
            instanceName,
            profileId
        )
    }

    /**
     * Formats the display name that is shows the host name for custom instances and the display name otherwise.
     *
     * @param instance The instance to format.
     * @return A shorter more legible version of the URL.
     */
    fun formatDisplayName(instance: Instance): String? {
        return if (instance.isCustom) {
            val uri = URI.create(instance.sanitizedBaseURI)
            uri.host
        } else if (instance.countryCode != null) {
            Locale("en", instance.countryCode).getDisplayCountry(Constants.ENGLISH_LOCALE)
        } else {
            instance.displayName.bestTranslation
        }
    }
}
