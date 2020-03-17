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

package nl.eduvpn.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import nl.eduvpn.app.adapter.viewholder.ProviderViewHolder;
import nl.eduvpn.app.databinding.ListItemProviderBinding;
import nl.eduvpn.app.entity.AuthorizationType;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.service.ConfigurationService;

/**
 * Adapter for the providers list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class AuthTypeProviderAdapter extends InstanceAdapter {

    private List<Instance> _instanceList;
    private LayoutInflater _layoutInflater;
    private AuthorizationType _authorizationType;

    public AuthTypeProviderAdapter(final ConfigurationService configurationService, AuthorizationType authorizationType) {
        _authorizationType = authorizationType;
        if (authorizationType == AuthorizationType.Local) {
            _instanceList = configurationService.getInstituteAccessList();
        } else if (authorizationType == AuthorizationType.Distributed) {
            _instanceList = configurationService.getSecureInternetList();
        }
        configurationService.addObserver((o, arg) -> {
            if (_authorizationType == AuthorizationType.Local) {
                _instanceList = configurationService.getInstituteAccessList();
            } else {
                _instanceList = configurationService.getSecureInternetList();
            }
            notifyDataSetChanged();
        });
    }

    /**
     * Returns the item at the given position.
     *
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
        holder.bind(instance);
    }

    @Override
    public int getItemCount() {
        return _instanceList == null ? 0 : _instanceList.size();
    }
}