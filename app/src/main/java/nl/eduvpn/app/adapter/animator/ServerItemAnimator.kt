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

package nl.eduvpn.app.adapter.animator

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import nl.eduvpn.app.adapter.ServerAdapter

class ServerItemAnimator : DefaultItemAnimator() {

    override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
        return true
    }

    override fun recordPreLayoutInformation(state: RecyclerView.State, viewHolder: RecyclerView.ViewHolder, changeFlags: Int, payloads: MutableList<Any>): ItemHolderInfo {
        val info = super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads) as ServerHolderInfo
        info.doExpand = payloads.contains(EXPAND_DETAIL)
        info.doCollapse = payloads.contains(COLLAPSE_DETAIL)
        return info
    }

    override fun animateChange(oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder, preInfo: ItemHolderInfo, postInfo: ItemHolderInfo): Boolean {
        if (newHolder is ServerAdapter.ServerParentViewHolder && preInfo is ServerHolderInfo) {
            if (preInfo.doExpand) {
                newHolder.expand()
            } else if (preInfo.doCollapse) {
                newHolder.collapse()
            }
        }
        return super.animateChange(oldHolder, newHolder, preInfo, postInfo)
    }

    override fun obtainHolderInfo(): ItemHolderInfo {
        return ServerHolderInfo()
    }

    private class ServerHolderInfo : ItemHolderInfo() {
        internal var doExpand: Boolean = false
        internal var doCollapse: Boolean = false
    }

    companion object {
        const val EXPAND_DETAIL = 1
        const val COLLAPSE_DETAIL = 2
    }
}