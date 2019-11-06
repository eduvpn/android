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

package nl.eduvpn.app.adapter.viewholder


import android.text.TextUtils
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import nl.eduvpn.app.R
import nl.eduvpn.app.databinding.ListItemProfileBinding
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Profile

/**
 * View holder for the provider instance list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class ProfileViewHolder(private val binding: ListItemProfileBinding) : RecyclerView.ViewHolder(binding.getRoot()) {

    fun bind(instance: Instance, profile: Profile) {
        binding.profileName.text = profile.displayName
        binding.profileProvider.text = instance.displayName
        if (!TextUtils.isEmpty(instance.logoUri)) {
            Picasso.get()
                    .load(instance.logoUri)
                    .fit()
                    .into(binding.profileIcon)
        } else {
            binding.profileIcon.setImageResource(R.drawable.external_provider)
        }
    }
}
