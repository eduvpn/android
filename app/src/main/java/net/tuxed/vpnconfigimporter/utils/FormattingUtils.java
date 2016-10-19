package net.tuxed.vpnconfigimporter.utils;

import android.content.Context;

import net.tuxed.vpnconfigimporter.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;

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
            return context.getString(R.string.traffic_kilobytes, megaByteString);
        } else {
            double gigaBytes = bytes.doubleValue() / BYTES_IN_A_GB;
            String gigaByteString = DECIMAL_FORMAT.format(gigaBytes);
            return context.getString(R.string.traffic_gigabytes, gigaByteString);
        }
    }
}
