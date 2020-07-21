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

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import nl.eduvpn.app.Constants
import nl.eduvpn.app.adapter.OrganizationAdapter
import nl.eduvpn.app.databinding.ListItemHeaderBinding
import nl.eduvpn.app.databinding.ListItemServerBinding
import nl.eduvpn.app.entity.Instance
import nl.eduvpn.app.entity.Organization
import nl.eduvpn.app.utils.FormattingUtils
import java.text.Format
import java.util.Locale

/**
 * Viewholder for the organization adapter items.
 * Created by Daniel Zolnai on 2016-10-07.
 */
abstract class OrganizationViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

class OrganizationHeaderViewHolder(private val binding: ListItemHeaderBinding) : OrganizationViewHolder(binding) {
    fun bind(header: OrganizationAdapter.OrganizationAdapterItem.Header) {
        binding.headerName.setText(header.headerName)
        binding.icon.setImageResource(header.icon)
        binding.changeLocation.isVisible = header.includeLocationButton
    }

    fun setOnChangeLocationClickListener(clickListener: (() -> Unit)?) {
        binding.changeLocation.setOnClickListener {
            clickListener?.invoke()
        }
    }
}

class OrganizationServerViewHolder(private val binding: ListItemServerBinding) : OrganizationViewHolder(binding) {
    fun bind(instance: Instance) {
        if (instance.countryCode != null) {
            binding.displayName.text = Locale("en", instance.countryCode).getDisplayCountry(Constants.ENGLISH_LOCALE)
        } else {
            binding.displayName.text = FormattingUtils.formatDisplayName(instance)
        }
    }

    fun bind(organization: Organization) {
        binding.displayName.text = organization.displayName.bestTranslation
    }

    fun bind(url: String) {
        binding.displayName.text = url
    }
}
