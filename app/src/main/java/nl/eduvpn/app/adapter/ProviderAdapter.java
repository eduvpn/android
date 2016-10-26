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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import nl.eduvpn.app.R;
import nl.eduvpn.app.adapter.viewholder.ProviderViewHolder;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.service.ConfigurationService;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Adapter for the providers list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class ProviderAdapter extends RecyclerView.Adapter<ProviderViewHolder> {

    private List<Instance> _instanceList;
    private LayoutInflater _layoutInflater;

    public ProviderAdapter(final ConfigurationService configurationService) {
        _instanceList = configurationService.getInstanceList().getInstanceList();
        configurationService.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                _instanceList = configurationService.getInstanceList().getInstanceList();
                notifyDataSetChanged();
            }
        });
    }

    /**
     * Returns the item at the given position.
     * @param position The position of the item.
     * @return The item at the given position. Null if 'Other' item or invalid query.
     */
    public Instance getItem(int position) {
        if (_instanceList == null) {
            return null;
        } else if (position < _instanceList.size()) {
            return _instanceList.get(position);
        } else {
            return null;
        }
    }


    @Override
    public ProviderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (_layoutInflater == null) {
            _layoutInflater = LayoutInflater.from(parent.getContext());
        }
        View view = _layoutInflater.inflate(R.layout.list_item_provider, parent, false);
        return new ProviderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ProviderViewHolder holder, int position) {
        if (_instanceList == null || position == _instanceList.size()) {
            // Other item
            holder.providerDisplayName.setText(R.string.provider_other_display_name);
            holder.providerIcon.setImageResource(R.drawable.other_vpn_icon);
        } else {
            // Usual item
            Instance instance = getItem(position);
            holder.providerDisplayName.setText(instance.getDisplayName());

            if (instance.getLogoUri() != null) {
                Picasso.with(holder.providerIcon.getContext())
                        .load(instance.getLogoUri())
                        .placeholder(R.drawable.vpn_icon)
                        .fit()
                        .into(holder.providerIcon);
            } else{
                holder.providerIcon.setImageResource(R.drawable.vpn_icon);
            }
        }
    }

    @Override
    public int getItemCount() {
        // Add one extra item which will be the 'Other...'
        return _instanceList == null ? 1 : _instanceList.size() + 1;
    }
}
