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

package nl.eduvpn.app.utils;

import android.content.Context;

import java.net.URI;
import java.text.DecimalFormat;

import nl.eduvpn.app.R;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.Profile;

/**
 * Utility methods for different formatting cases.
 * Created by Daniel Zolnai on 2016-10-18.
 */
public class FormattingUtils {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private static final long BYTES_IN_A_KB = 1024;
    private static final long BYTES_IN_A_MB = BYTES_IN_A_KB * 1024;
    private static final long BYTES_IN_A_GB = BYTES_IN_A_MB * 1024;

    /**
     * Formats the output which displays that for how long the VPN was connected.
     *
     * @param context The application or activity context.
     * @param seconds The seconds which have elapsed since the connection was initialized. Use null for the not connected status.
     * @return The string to display in the UI.
     */
    public static String formatDurationSeconds(Context context, Long seconds) {
        if (seconds == null) {
            return context.getString(R.string.not_available);
        } else if (seconds >= 3600) {
            long hours = seconds / 3600;
            long minutes = (seconds / 60) % 60;
            long secondsInMinute = seconds % 60;
            return context.getString(R.string.duration_more_than_one_hour, hours, minutes, secondsInMinute);
        } else {
            long minutes = (seconds / 60) % 60;
            long secondsInMinute = seconds % 60;
            return context.getString(R.string.duration_less_than_one_hour, minutes, secondsInMinute);
        }
    }

    /**
     * Formats the bytes traffic to a more readable format.
     *
     * @param context The application or activity context.
     * @param bytes   The number of bytes.
     * @return The human readable format.
     */
    public static String formatBytesTraffic(Context context, Long bytes) {
        if (bytes == null) {
            return context.getString(R.string.not_available);
        } else if (bytes < BYTES_IN_A_MB) {
            double kiloBytes = bytes.doubleValue() / BYTES_IN_A_KB;
            String kiloByteString = DECIMAL_FORMAT.format(kiloBytes);
            return context.getString(R.string.traffic_kilobytes, kiloByteString);
        } else if (bytes < BYTES_IN_A_GB) {
            double megaBytes = bytes.doubleValue() / BYTES_IN_A_MB;
            String megaByteString = DECIMAL_FORMAT.format(megaBytes);
            return context.getString(R.string.traffic_megabytes, megaByteString);
        } else {
            double gigaBytes = bytes.doubleValue() / BYTES_IN_A_GB;
            String gigaByteString = DECIMAL_FORMAT.format(gigaBytes);
            return context.getString(R.string.traffic_gigabytes, gigaByteString);
        }
    }

    /**
     * Creates a name to display in the list of saved profiles.
     *
     * @param context  The application or activity context.
     * @param instance The provider which gives the first part of the name
     * @param profile  The profile which gives the second part of the name.
     * @return The name to display.
     */
    public static String formatProfileName(Context context, Instance instance, Profile profile) {
        String instanceName = formatDisplayName(instance);
        return context.getString(R.string.saved_profile_display_name, instanceName, profile.getDisplayName()
                .getBestTranslation());
    }

    /**
     * Formats the display name that is shows the host name for custom instances and the display name otherwise.
     *
     * @param instance The instance to format.
     * @return A shorter more legible version of the URL.
     */
    public static String formatDisplayName(Instance instance) {
        if (instance.isCustom()) {
            URI uri = URI.create(instance.getSanitizedBaseURI());
            return uri.getHost();
        } else {
            return instance.getDisplayName().getBestTranslation();
        }

    }
}
