package net.tuxed.vpnconfigimporter.utils;

import android.content.Context;
import android.support.annotation.StringRes;

/**
 * Utility class for displaying error dialogs through the entire application.
 * Created by Daniel Zolnai on 2016-10-12.
 */
public class ErrorDialog {

    public static void show(Context context, @StringRes int title, @StringRes int message) {
        show(context, context.getString(title), context.getString(message));
    }

    public static void show(Context context, String title, String message) {
        // TODO
    }
}
