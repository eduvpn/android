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
import nl.eduvpn.app.adapter.viewholder.ProfileViewHolder
import nl.eduvpn.app.databinding.ListItemProfileBinding
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Profile

/**
 * Adapter for the profile list.
 * Created by Daniel Zolnai on 2016-10-11.
 */
class ProfileAdapter(private val instance: Instance) : ListAdapter<Profile, ProfileViewHolder>(object : DiffUtil.ItemCallback<Profile>() {
    override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
        return oldItem.profileId == newItem.profileId
    }

    override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean {
        return oldItem.profileId == newItem.profileId && oldItem.displayName == newItem.displayName
    }
}) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        return ProfileViewHolder(ListItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    public override fun getItem(position: Int): Profile {
        return super.getItem(position)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val profile = getItem(position)
        holder.bind(instance, profile)
    }
}
