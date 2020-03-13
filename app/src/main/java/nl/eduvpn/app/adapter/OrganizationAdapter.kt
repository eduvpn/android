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
import androidx.recyclerview.widget.RecyclerView
import nl.eduvpn.app.adapter.viewholder.OrganizationViewHolder
import nl.eduvpn.app.databinding.ListItemOrganizationBinding
import nl.eduvpn.app.entity.Organization
import nl.eduvpn.app.service.ConfigurationService
import java.util.Observable
import kotlin.math.min

/**
 * Adapter for the providers list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class OrganizationAdapter(private val configurationService: ConfigurationService) : RecyclerView.Adapter<OrganizationViewHolder>() {

    private val unfilteredList: MutableList<Organization> = mutableListOf()
    private val filteredList: MutableList<Organization> = mutableListOf()
    private var layoutInflater: LayoutInflater? = null
    val isDiscoveryPending: Boolean
        get() = configurationService.isPendingOrganizationsDiscovery


    var searchFilter: String? = null
        set(value) {
            field = value
            updateFilteredList()
        }


    init {
        unfilteredList.addAll(configurationService.organizationList)
        configurationService.addObserver { _: Observable?, _: Any? ->
            unfilteredList.clear()
            unfilteredList.addAll(configurationService.organizationList)
            updateFilteredList()
        }
    }

    private fun updateFilteredList() {
        filteredList.clear()
        val filter = searchFilter
        if (filter.isNullOrEmpty()) {
            filteredList.addAll(unfilteredList)
        } else {
            filteredList.addAll(unfilteredList.filter {
                it.displayName.contains(filter, ignoreCase = true) || it.keywordList.any { keyword -> keyword.equals(filter, ignoreCase = true) }
            })
        }
        notifyDataSetChanged()
    }

    /**
     * Returns the item at the given position.
     *
     * @param position The position of the item.
     * @return The item at the given position. Null if 'Other' item or invalid query.
     */
    fun getItem(position: Int): Organization? {
        return if (position < filteredList.size) {
            filteredList[position]
        } else {
            null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrganizationViewHolder {
        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.context)
        }
        return OrganizationViewHolder(ListItemOrganizationBinding.inflate(layoutInflater!!, parent, false))
    }

    override fun onBindViewHolder(holder: OrganizationViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

}