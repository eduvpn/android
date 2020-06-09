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
package nl.eduvpn.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import nl.eduvpn.app.adapter.viewholder.OrganizationHeaderViewHolder
import nl.eduvpn.app.adapter.viewholder.OrganizationServerViewHolder
import nl.eduvpn.app.adapter.viewholder.OrganizationViewHolder
import nl.eduvpn.app.databinding.ListItemHeaderBinding
import nl.eduvpn.app.databinding.ListItemServerBinding
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Organization

/**
 * Adapter for the providers list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class OrganizationAdapter : ListAdapter<OrganizationAdapter.OrganizationAdapterItem, OrganizationViewHolder>(object : DiffUtil.ItemCallback<OrganizationAdapterItem>() {
    override fun areContentsTheSame(oldItem: OrganizationAdapterItem, newItem: OrganizationAdapterItem): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(oldItem: OrganizationAdapterItem, newItem: OrganizationAdapterItem): Boolean {
        return oldItem == newItem
    }
}) {

    sealed class OrganizationAdapterItem {
        data class Header(@DrawableRes val icon: Int, @StringRes val headerName: Int) : OrganizationAdapterItem()
        data class InstituteAccess(val server: Instance) : OrganizationAdapterItem()
        data class SecureInternet(val server: Instance, val organization: Organization?) : OrganizationAdapterItem()
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is OrganizationAdapterItem.Header) {
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_SERVER
        }
    }

    override fun onBindViewHolder(holder: OrganizationViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is OrganizationHeaderViewHolder) {
            holder.bind(getItem(position) as OrganizationAdapterItem.Header)
        } else if (holder is OrganizationServerViewHolder) {
            if (item is OrganizationAdapterItem.InstituteAccess) {
                holder.bind(item.server)
            } else if (item is OrganizationAdapterItem.SecureInternet) {
                if (item.organization != null ) {
                    holder.bind(item.organization)
                } else {
                    holder.bind(item.server)
                }
            } else {
                throw RuntimeException("Unexpected item type: $item")
            }

        }
    }

    public override fun getItem(position: Int): OrganizationAdapterItem {
        return super.getItem(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrganizationViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            OrganizationHeaderViewHolder(ListItemHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            OrganizationServerViewHolder(ListItemServerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_SERVER = 1
    }

}