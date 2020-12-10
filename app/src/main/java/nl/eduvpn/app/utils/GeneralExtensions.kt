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

package nl.eduvpn.app.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import nl.eduvpn.app.Constants
import nl.eduvpn.app.entity.Instance
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


/**
 * Extension method to get inputManager for Context.
 */
inline val Context.inputManager: InputMethodManager
    get() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager


/**
 * Extension method to provide show keyboard for [View].
 */
fun View.showKeyboard() {
    this.requestFocus()
    context.inputManager.showSoftInput(this, 0)
}

/**
 * Extension method to provide hide keyboard for [View].
 */
fun View.hideKeyboard(clearFocus: Boolean = true) {
    if (clearFocus) {
        this.clearFocus()
    }
    context.inputManager.hideSoftInputFromWindow(windowToken, 0)
}

/**
 * Converts LiveData to emit single events only.
 */
fun <T> LiveData<T>.toSingleEvent(): LiveData<T> {
    val result = LiveEvent<T>()
    result.addSource(this) {
        result.value = it
    }
    return result
}

fun Instance.getCountryText(): String? {
    if (countryCode == null) {
        return null
    }
    val countryName = Locale("en", countryCode).getDisplayCountry(Constants.ENGLISH_LOCALE)
    val firstLetter: Int = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
    val secondLetter: Int = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
    val countryEmoji = String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    return "$countryEmoji   $countryName"
}

/**
 * Extension method for OkHttp for integration with coroutines.
 */
suspend fun Call.await(): Response {
    return withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation {
                kotlin.runCatching {
                    cancel()
                }
            }
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    cont.resume(response)
                }
            })
        }
    }
}

/**
 * [kotlin.runCatching] that does not catch CancellationException.
 * See https://github.com/Kotlin/kotlinx.coroutines/issues/1814
 */
inline fun <R> runCatchingCoroutine(block: () -> R): Result<R> {
    val result = kotlin.runCatching(block)
    result.onFailure { thr ->
        if (thr is CancellationException) {
            throw thr
        }
    }
    return result
}
