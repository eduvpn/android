package net.tuxed.vpnconfigimporter.utils;

import android.content.Context;

import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.entity.SavedProfile;
import net.tuxed.vpnconfigimporter.entity.message.Maintenance;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Utility methods for different formatting cases.
 * Created by Daniel Zolnai on 2016-10-18.
 */
public class FormattingUtils {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private static final DateFormat MAINTENANCE_DATE_FORMAT = new SimpleDateFormat("EEE M/dd HH:mm", Locale.getDefault());

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
     * Return the default text for maintenance messages.
     *
     * @param context     The application or activity context.
     * @param maintenance The maintenance instance describing the conditions.
     * @return The string to display in the message contents.
     */
    public static String getMaintenanceText(Context context, Maintenance maintenance) {
        String beginDateString = MAINTENANCE_DATE_FORMAT.format(maintenance.getStart());
        String endDateString = MAINTENANCE_DATE_FORMAT.format(maintenance.getEnd());
        return context.getString(R.string.maintenance_message, beginDateString, endDateString);
    }

    /**
     * Creates a name to display in the list of saved profiles.
     *
     * @param context      The application or activity context.
     * @param savedProfile The saved profile to construct the name from.
     * @return The name to display.
     */
    public static String formatSavedProfileName(Context context, SavedProfile savedProfile) {
        return context.getString(R.string.saved_profile_display_name, savedProfile.getInstance().getDisplayName(),
                savedProfile.getProfile().getDisplayName());
    }

    /**
     * Creates a name to display in the notification which will be visible in the status bar when the VPN is connected.
     *
     * @param context  The application or activity context.
     * @param instance The VPN provider.
     * @param profile  The profile which was downloaded.
     * @return The name to display.
     */
    public static String formatVPNProfileName(Context context, Instance instance, Profile profile) {
        return context.getString(R.string.vpn_profile_name, instance.getDisplayName(), profile.getDisplayName());
    }
}
