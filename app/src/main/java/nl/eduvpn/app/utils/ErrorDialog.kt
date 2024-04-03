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

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import nl.eduvpn.app.R
import nl.eduvpn.app.entity.exception.CommonException
import nl.eduvpn.app.entity.exception.EduVPNException


/**
 * Utility class for displaying error dialogs through the entire application.
 * Created by Daniel Zolnai on 2016-10-12.
 */
object ErrorDialog {

    /**
     * Shows a new error dialog.
     *
     * @param activity The activity the dialog will be shown in.
     * @param title   The title of the dialog (string resource ID).
     * @param message The message to show as a string.
     */
    @JvmStatic
    fun show(activity: FragmentActivity, @StringRes title: Int, message: String): ErrorDialogFragment? {
        return show(activity, activity.getString(title), message)
    }

    /**
     * Shows a new error dialog.
     *
     * @param activity The activity the dialog will be shown in.
     * @param title   The title of the dialog (string resource ID).
     * @param message The message to show (string resource ID).
     */
    @JvmStatic
    fun show(activity: FragmentActivity, @StringRes title: Int, @StringRes message: Int): ErrorDialogFragment? {
        return show(activity, activity.getString(title), activity.getString(message))
    }

    @JvmStatic
    fun show(activity: FragmentActivity, thr: Throwable): ErrorDialogFragment? {
        val titleId = if (thr is EduVPNException) {
            thr.resourceIdTitle
        } else {
            R.string.error_dialog_title
        }
        val message = if (thr is EduVPNException) {
            activity.getString(thr.resourceIdMessage, *thr.formatArgs)
        } else if (thr is CommonException) {
            thr.translatedMessage()
        } else {
            thr.message ?: thr.toString()
        }
        return show(activity, titleId, message)
    }

    /**
     * Shows a new error dialog
     *
     * @param context The application or activity context.
     * @param title   The title of the dialog.
     * @param message The message to show.
     */
    @JvmStatic
    fun show(activity: FragmentActivity, title: String, message: String): ErrorDialogFragment? {
        if (activity.isFinishing) {
            return null
        }
        val dialog = ErrorDialogFragment()
        dialog.arguments = Bundle().apply {
            putString(ErrorDialogFragment.ARGS_KEY_TITLE, title)
            putString(ErrorDialogFragment.ARGS_KEY_MESSAGE, message)
        }
        dialog.show(activity.supportFragmentManager, null)
        return dialog
    }

    @Suppress("DEPRECATION")
    fun hasNetworkConnection(context: Context) : Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo
        return activeNetworkInfo?.isConnected == true
    }

    class ErrorDialogFragment : DialogFragment() {
        interface Listener {
            fun onDismiss()
        }

        var listener: Listener? = null
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.dialog_error, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val title = arguments?.getString(ARGS_KEY_TITLE)
            val message = arguments?.getString(ARGS_KEY_MESSAGE)
            val titleView = view.findViewById<TextView>(R.id.title)
            val errorTextView = view.findViewById<TextView>(R.id.errorText)
            val confirmButton = view.findViewById<Button>(R.id.confirmButton)
            if (hasNetworkConnection(requireContext())) {
                titleView.text = title
                errorTextView.text = message
            } else {
                titleView.setText(R.string.error_no_internet_connection)
                errorTextView.text = requireContext().getString(R.string.error_no_internet_connection_message, message)
            }
            confirmButton.setOnClickListener { dismiss() }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = super.onCreateDialog(savedInstanceState)
            dialog.setCanceledOnTouchOutside(true)
            return dialog
        }

        override fun onDismiss(dialog: DialogInterface) {
            super.onDismiss(dialog)
            listener?.onDismiss()
        }

        companion object {
            const val ARGS_KEY_TITLE = "dialog_title"
            const val ARGS_KEY_MESSAGE = "dialog_message"
        }
    }
}
