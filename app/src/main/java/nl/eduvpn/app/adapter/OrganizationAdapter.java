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

package nl.eduvpn.app.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import nl.eduvpn.app.R;
import nl.eduvpn.app.adapter.viewholder.ProviderViewHolder;
import nl.eduvpn.app.databinding.ListItemProviderBinding;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.service.ConfigurationService;

/**
 * Adapter for the providers list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class OrganizationAdapter extends RecyclerView.Adapter<ProviderViewHolder> {

    private List<Instance> _organizationList;
    private LayoutInflater _layoutInflater;

    private ConfigurationService _configurationService;

    public OrganizationAdapter(final ConfigurationService configurationService) {
        _configurationService = configurationService;
        _organizationList = configurationService.getOrganizationList();
        configurationService.addObserver((o, arg) -> {
            _organizationList = configurationService.getOrganizationList();
            notifyDataSetChanged();
        });
    }

    public boolean isDiscoveryPending() {
        return _configurationService.isPendingOrganizationsDiscovery();
    }

    /**
     * Returns the item at the given position.
     *
     * @param position The position of the item.
     * @return The item at the given position. Null if 'Other' item or invalid query.
     */
    public Instance getItem(int position) {
        if (_organizationList == null) {
            return null;
        } else if (position < _organizationList.size()) {
            return _organizationList.get(position);
        } else {
            return null;
        }
    }


    @NonNull
    @Override
    public ProviderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (_layoutInflater == null) {
            _layoutInflater = LayoutInflater.from(parent.getContext());
        }
        return new ProviderViewHolder(ListItemProviderBinding.inflate(_layoutInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(ProviderViewHolder holder, int position) {
        Instance instance = getItem(position);
        holder.providerDisplayName.setText(instance.getDisplayName());
        if (!TextUtils.isEmpty(instance.getLogoUri())) {
            Picasso.get()
                    .load(instance.getLogoUri())
                    .fit()
                    .into(holder.providerIcon);
        } else {
            holder.providerIcon.setImageResource(R.drawable.external_provider);
        }
    }

    @Override
    public int getItemCount() {
        return _organizationList == null ? 0 : _organizationList.size();
    }
}
