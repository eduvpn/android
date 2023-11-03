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

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import nl.eduvpn.app.R

/**
 * Adds item click support to a RecyclerView.
 * Taken from: http://www.littlerobots.nl/blog/Handle-Android-RecyclerView-Clicks/
 * Created by Daniel Zolnai on 2016-10-07.
 */
class ItemClickSupport private constructor(private val recyclerView: RecyclerView) {
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null
    private val onClickListener = View.OnClickListener { v ->
        onItemClickListener?.let {
            val holder = recyclerView.getChildViewHolder(v)
            it.onItemClicked(recyclerView, holder.bindingAdapterPosition, v)
        }
    }
    private val onLongClickListener = View.OnLongClickListener { v ->
        onItemLongClickListener?.let {
            val holder = recyclerView.getChildViewHolder(v)
            return@OnLongClickListener it.onItemLongClicked(recyclerView, holder.bindingAdapterPosition, v)
        }
        false
    }
    private val attachListener: RecyclerView.OnChildAttachStateChangeListener = object : RecyclerView.OnChildAttachStateChangeListener {
        override fun onChildViewAttachedToWindow(view: View) {
            if (onItemClickListener != null) {
                if (!view.hasOnClickListeners()) {
                    view.setOnClickListener(onClickListener)
                }
            }
            if (onItemLongClickListener != null) {
                view.setOnLongClickListener(onLongClickListener)
            }
        }

        override fun onChildViewDetachedFromWindow(view: View) {}
    }

    init {
        recyclerView.setTag(R.id.item_click_support, this)
        recyclerView.addOnChildAttachStateChangeListener(attachListener)
    }

    fun setOnItemClickListener(listener: OnItemClickListener): ItemClickSupport {
        onItemClickListener = listener
        return this
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener): ItemClickSupport {
        onItemLongClickListener = listener
        return this
    }

    private fun detach(view: RecyclerView) {
        view.removeOnChildAttachStateChangeListener(attachListener)
        view.setTag(R.id.item_click_support, null)
    }

    fun interface OnItemClickListener {
        fun onItemClicked(recyclerView: RecyclerView?, position: Int, v: View?)
    }

    fun interface OnItemLongClickListener {
        fun onItemLongClicked(recyclerView: RecyclerView?, position: Int, v: View?): Boolean
    }

    companion object {
        fun addTo(view: RecyclerView): ItemClickSupport {
            var support = view.getTag(R.id.item_click_support) as? ItemClickSupport
            if (support == null) {
                support = ItemClickSupport(view)
            }
            return support
        }

        fun removeFrom(view: RecyclerView): ItemClickSupport? {
            val support = view.getTag(R.id.item_click_support) as? ItemClickSupport
            support?.detach(view)
            return support
        }
    }
}