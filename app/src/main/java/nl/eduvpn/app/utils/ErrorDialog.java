package nl.eduvpn.app.utils;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
    public static void show(Context context, @StringRes int title, String message) {
        show(context, context.getString(title), message);
    }

    /**
     * Shows a new error dialog.
     *
     * @param context The application or activity context.
     * @param title   The title of the dialog (string resource ID).
     * @param message The message to show (string resource ID).
     */
    public static void show(Context context, @StringRes int title, @StringRes int message) {
        show(context, context.getString(title), context.getString(message));
    }

    /**
     * Shows a new error dialog
     *
     * @param context The application or activity context.
     * @param title   The title of the dialog.
     * @param message The message to show.
     */
    public static void show(Context context, String title, String message) {
        final Dialog dialog = new Dialog(context, R.style.ErrorDialog);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.dialog_error);
        View view = dialog.findViewById(R.id.errorDialog);
        TextView titleView = (TextView)view.findViewById(R.id.title);
        TextView errorTextView = (TextView)view.findViewById(R.id.errorText);
        Button confirmButton = (Button)view.findViewById(R.id.confirmButton);
        titleView.setText(title);
        errorTextView.setText(message);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
