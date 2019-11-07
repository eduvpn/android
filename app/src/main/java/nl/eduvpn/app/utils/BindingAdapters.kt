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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import nl.eduvpn.app.R

@BindingAdapter("animatedVisibility", "showDelay", requireAll = false)
fun View.setAnimatedVisibilityWithDelay(newVisibility: Int, showDelay: Int = -1) {
    if (showDelay > 0 && newVisibility == View.VISIBLE) {
        (getTag(R.id.delayed_visibility_runnable) as? Runnable)?.let {
            return
        }
        val delayingRunnable = Runnable {
            this.setTag(R.id.delayed_visibility_runnable, null)
            setAnimatedVisibility(newVisibility)
        }
        setTag(R.id.delayed_visibility_runnable, delayingRunnable)
        postDelayed(delayingRunnable, showDelay.toLong())
    } else {
        (getTag(R.id.delayed_visibility_runnable) as? Runnable)?.let {
            setTag(R.id.delayed_visibility_runnable, null)
            this.removeCallbacks(it)
        }
        setAnimatedVisibility(newVisibility)
    }
}

// Taken from: https://medium.com/google-developers/android-data-binding-animations-55f6b5956a64
fun View.setAnimatedVisibility(newVisibility: Int) {
    // Were we animating before? If so, what was the visibility?
    val endAnimVisibility = getTag(R.id.final_visibility) as Int?
    val oldVisibility = endAnimVisibility ?: visibility

    if (oldVisibility == newVisibility) {
        // just let it finish any current animation.
        return
    }

    val isVisible = oldVisibility == View.VISIBLE
    val willBeVisible = newVisibility == View.VISIBLE

    this.visibility = View.VISIBLE
    val startAlpha = if (isVisible) alpha else 0f
    val endAlpha = if (willBeVisible) 1f else 0f

    // Now create an animator
    val animator = ObjectAnimator.ofFloat(this, View.ALPHA, startAlpha, endAlpha)
    animator.setAutoCancel(true)

    animator.addListener(object : AnimatorListenerAdapter() {
        private var isCanceled: Boolean = false

        override fun onAnimationStart(anim: Animator?) {
            setTag(R.id.final_visibility, newVisibility)
        }

        override fun onAnimationCancel(anim: Animator?) {
            isCanceled = true
        }

        override fun onAnimationEnd(anim: Animator?) {
            setTag(R.id.final_visibility, null)
            if (!isCanceled) {
                alpha = 1f
                visibility = newVisibility
            }
        }
    })
    animator.start()
}

@BindingAdapter("android:text", "dontChangeIfNull", requireAll = false)
fun TextView.setTextResource(resource: Int?, dontChangeIfNull: Boolean = false) {
    if (resource == null || resource == 0) {
        if (!dontChangeIfNull) {
            text = null
        }
    } else {
        setText(resource)
    }
}
