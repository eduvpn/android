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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import nl.eduvpn.app.adapter.viewholder.ServerViewHolder
import nl.eduvpn.app.databinding.ListItemServerBinding
import nl.eduvpn.app.entity.Instance

/**
 * Adapter for the providers list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class ServerAdapter : ListAdapter<Instance, ServerViewHolder>(object : DiffUtil.ItemCallback<Instance>() {
    override fun areItemsTheSame(oldItem: Instance, newItem: Instance): Boolean {
        return oldItem.sanitizedBaseURI == newItem.sanitizedBaseURI
    }

    override fun areContentsTheSame(oldItem: Instance, newItem: Instance): Boolean {
        return oldItem.sanitizedBaseURI == newItem.sanitizedBaseURI && oldItem.displayName == newItem.displayName
    }
}) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        return ServerViewHolder(ListItemServerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val instance = getItem(position)
        holder.bind(instance)
    }

    public override fun getItem(position: Int): Instance {
        return super.getItem(position)
    }
}
