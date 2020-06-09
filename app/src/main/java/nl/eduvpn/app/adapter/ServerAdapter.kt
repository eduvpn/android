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

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.squareup.picasso.Picasso
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.viewholder.ServerChildViewHolder
import nl.eduvpn.app.adapter.viewholder.ServerViewHolder
import nl.eduvpn.app.databinding.ListItemServerBinding
import nl.eduvpn.app.entity.DiscoveredInstance
import nl.eduvpn.app.utils.FormattingUtils

/**
 * Adapter for the providers list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class ServerAdapter(context: Context) : ListAdapter<DiscoveredInstance, ServerViewHolder>(object : DiffUtil.ItemCallback<DiscoveredInstance>() {
    override fun areItemsTheSame(oldItem: DiscoveredInstance, newItem: DiscoveredInstance): Boolean {
        return oldItem.instance.sanitizedBaseURI == newItem.instance.sanitizedBaseURI
    }

    override fun areContentsTheSame(oldItem: DiscoveredInstance, newItem: DiscoveredInstance): Boolean {
        return oldItem == newItem
    }
}) {

    private val layoutInflater = LayoutInflater.from(context)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val binding = ListItemServerBinding.inflate(layoutInflater, parent, false)
        return ServerParentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val instance = getItem(position)
        if (holder is ServerParentViewHolder) {
            holder.bind(instance)
        } else if (holder is ServerChildViewHolder) {
            holder.bind(instance)
        }
    }

    public override fun getItem(position: Int): DiscoveredInstance {
        return super.getItem(position)
    }

    inner class ServerParentViewHolder(private val binding: ListItemServerBinding) : ServerViewHolder(binding) {

        fun bind(discoveredInstance: DiscoveredInstance) {
            val instance = discoveredInstance.instance
            binding.displayName.text = FormattingUtils.formatDisplayName(instance)
            /**
            if (!TextUtils.isEmpty(instance.logoUri)) {
                Picasso.get()
                        .load(instance.logoUri)
                        .fit()
                        .into(binding.serverIcon)
            } else if (instance.isCustom) {
                binding.serverIcon.setImageResource(R.drawable.ic_custom_url)
            }
            binding.serverIcon.setImageResource(R.drawable.ic_institute)
            if (discoveredInstance.isCachedOnly) {
                binding.serverName.alpha = 0.5f
                binding.serverIcon.alpha = 0.5f
            } else {
                binding.serverName.alpha = 1f
                binding.serverIcon.alpha = 1f
            }**/
        }
    }
}
