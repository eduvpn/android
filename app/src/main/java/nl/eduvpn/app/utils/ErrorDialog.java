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

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.StringRes;
import nl.eduvpn.app.R;

/**
 * Utility class for displaying error dialogs through the entire application.
 * Created by Daniel Zolnai on 2016-10-12.
 */
public class ErrorDialog {

    /**
     * Shows a new error dialog.
     *
     * @param context The application or activity context.
     * @param title   The title of the dialog (string resource ID).
     * @param message The message to show as a string.
     */
    public static Dialog show(Context context, @StringRes int title, String message) {
        return show(context, context.getString(title), message);
    }

    /**
     * Shows a new error dialog.
     *
     * @param context The application or activity context.
     * @param title   The title of the dialog (string resource ID).
     * @param message The message to show (string resource ID).
     */
    public static Dialog show(Context context, @StringRes int title, @StringRes int message) {
        return show(context, context.getString(title), context.getString(message));

    }

    /**
     * Shows a new error dialog
     *
     * @param context The application or activity context.
     * @param title   The title of the dialog.
     * @param message The message to show.
     */
    public static Dialog show(Context context, String title, String message) {
        final Dialog dialog = new Dialog(context, R.style.ErrorDialog);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.dialog_error);
        View view = dialog.findViewById(R.id.errorDialog);
        TextView titleView = view.findViewById(R.id.title);
        TextView errorTextView = view.findViewById(R.id.errorText);
        Button confirmButton = view.findViewById(R.id.confirmButton);
        titleView.setText(title);
        errorTextView.setText(message);
        confirmButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        return dialog;
    }
}
