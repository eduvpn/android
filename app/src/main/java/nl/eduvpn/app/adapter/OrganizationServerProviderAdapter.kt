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
package nl.eduvpn.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import nl.eduvpn.app.adapter.viewholder.ProviderViewHolder
import nl.eduvpn.app.databinding.ListItemProviderBinding
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.service.OrganizationService
import java.util.Observable

/**
 * Adapter for the providers list.
 * Created by Daniel Zolnai on 2020-03-16.
 */
class OrganizationServerProviderAdapter : InstanceAdapter() {
    private var instanceList: List<Instance>? = null
    private var layoutInflater: LayoutInflater? = null

    override fun getItem(position: Int): Instance? {
        return instanceList?.getOrNull(position)
    }

    fun setInstances(instances: List<Instance>) {
        instanceList = instances
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.context)
        }
        return ProviderViewHolder(ListItemProviderBinding.inflate(layoutInflater!!, parent, false))
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val instance = getItem(position)
        holder.bind(instance!!)
    }

    override fun getItemCount(): Int {
        return instanceList?.size ?: 0
    }
}