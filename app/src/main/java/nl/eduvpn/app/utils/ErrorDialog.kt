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

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.exception.EduVPNException


/**
 * Utility class for displaying error dialogs through the entire application.
 * Created by Daniel Zolnai on 2016-10-12.
 */
object ErrorDialog {

    /**
     * Shows a new error dialog.
     *
     * @param context The application or activity context.
     * @param title   The title of the dialog (string resource ID).
     * @param message The message to show as a string.
     */
    @JvmStatic
    fun show(context: Context, @StringRes title: Int, message: String): Dialog? {
        return show(context, context.getString(title), message)
    }

    /**
     * Shows a new error dialog.
     *
     * @param context The application or activity context.
     * @param title   The title of the dialog (string resource ID).
     * @param message The message to show (string resource ID).
     */
    @JvmStatic
    fun show(context: Context, @StringRes title: Int, @StringRes message: Int): Dialog? {
        return show(context, context.getString(title), context.getString(message))
    }

    @JvmStatic
    fun show(context: Context, thr: Throwable): Dialog? {
        val titleId = if (thr is EduVPNException) {
            thr.resourceIdTitle
        } else {
            R.string.error_dialog_title
        }
        val message = if (thr is EduVPNException) {
            context.getString(thr.resourceIdMessage, thr.throwable)
        } else {
            thr.localizedMessage!!
        }
        return show(context, titleId, message)
    }

    /**
     * Shows a new error dialog
     *
     * @param context The application or activity context.
     * @param title   The title of the dialog.
     * @param message The message to show.
     */
    @JvmStatic
    fun show(context: Context, title: String, message: String): Dialog? {
        if (context !is Activity || context.isFinishing) {
            return null
        }
        val dialog = Dialog(context, R.style.ErrorDialog)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_error)
        val view = dialog.findViewById<View>(R.id.errorDialog)
        val titleView = view.findViewById<TextView>(R.id.title)
        val errorTextView = view.findViewById<TextView>(R.id.errorText)
        val confirmButton = view.findViewById<Button>(R.id.confirmButton)
        if (hasNetworkConnection(context)) {
            titleView.text = title
            errorTextView.text = message
        } else {
            titleView.setText(R.string.error_no_internet_connection)
            errorTextView.text = context.getString(R.string.error_no_internet_connection_message, message)
        }
        confirmButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
        return dialog
    }

    @Suppress("DEPRECATION")
    fun hasNetworkConnection(context: Context) : Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo
        return activeNetworkInfo?.isConnected == true
    }
}
