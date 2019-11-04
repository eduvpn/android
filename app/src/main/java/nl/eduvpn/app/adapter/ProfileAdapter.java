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
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import nl.eduvpn.app.R;
import nl.eduvpn.app.adapter.viewholder.ProfileViewHolder;
import nl.eduvpn.app.databinding.ListItemConfigBinding;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.Profile;
import nl.eduvpn.app.service.HistoryService;
import nl.eduvpn.app.utils.FormattingUtils;

/**
 * Adapter for the profile list.
 * Created by Daniel Zolnai on 2016-10-11.
 */
public class ProfileAdapter extends RecyclerView.Adapter<ProfileViewHolder> {

    private List<Pair<Instance, Profile>> _profileList;

    private final Object _profileListLock = new Object();

    private LayoutInflater _layoutInflater;
    private HistoryService _historyService;

    /**
     * Constructor.
     *
     * @param historyService The history service.
     * @param profileList    The list of instance and profile pairs to put in the list.
     */
    public ProfileAdapter(HistoryService historyService, @Nullable List<Pair<Instance, Profile>> profileList) {
        _historyService = historyService;
        _profileList = profileList;
        if (_profileList == null) {
            _profileList = new ArrayList<>();
        }
    }

    /**
     * Adds new items to this adapter.
     *
     * @param profiles The list of profiles to add to the list of current items.
     */
    public void addItemsIfNotAdded(List<Pair<Instance, Profile>> profiles) {
        synchronized (_profileListLock) {
            for (Pair<Instance, Profile> newPair : profiles) {
                ListIterator<Pair<Instance, Profile>> existingItemIterator = _profileList.listIterator();
                boolean replacedItem = false;
                while (existingItemIterator.hasNext() && !replacedItem) {
                    Pair<Instance, Profile> existingPair = existingItemIterator.next();
                    if (existingPair.first.getBaseURI().equals(newPair.first.getBaseURI()) &&
                            existingPair.second.getDisplayName().equals(newPair.second.getDisplayName())) {
                        // Replace the item
                        replacedItem = true;
                        existingItemIterator.set(newPair);
                    }
                }
                if (!replacedItem) {
                    _profileList.add(newPair);
                }
            }
        }
        notifyDataSetChanged();

    }


    /**
     * Returns the item at the given position.
     *
     * @param position The position of the item.
     * @return The item at the given position.
     */
    public Pair<Instance, Profile> getItem(int position) {
        return _profileList.get(position);
    }


    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (_layoutInflater == null) {
            _layoutInflater = LayoutInflater.from(parent.getContext());
        }
        return new ProfileViewHolder(ListItemConfigBinding.inflate(_layoutInflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        final Pair<Instance, Profile> instanceProfilePair = getItem(position);
            holder.profileName.setVisibility(View.VISIBLE);
            holder.providerIcon.setVisibility(View.VISIBLE);
            holder.profileName.setText(instanceProfilePair.second.getDisplayName());
            holder.profileProvider.setText(FormattingUtils.formatInstanceUrl(instanceProfilePair.first));
            if (!TextUtils.isEmpty(instanceProfilePair.first.getLogoUri())) {
                Picasso.get()
                        .load(instanceProfilePair.first.getLogoUri())
                        .fit()
                        .noFade()
                        .into(holder.providerIcon);
            } else {
                holder.providerIcon.setImageResource(R.drawable.external_provider);
            }
    }

    @Override
    public long getItemId(int position) {
        Pair<Instance, Profile> instanceProfilePair = getItem(position);
        return instanceProfilePair.first.getBaseURI().hashCode() +
                17 * instanceProfilePair.second.getProfileId().hashCode();
    }

    @Override
    public int getItemCount() {
        synchronized (_profileListLock) {
            return _profileList != null ? _profileList.size() : 0;
        }
    }



    /**
     * Removes the item at the given position.
     *
     * @param item The item to remove
     */
    public void remove(Pair<Instance, Profile> item) {
        synchronized (_profileListLock) {
            int firstIndex = -1;
            int lastIndex = -1;
            for (int i = 0; i < _profileList.size(); ++i) {
                Pair<Instance, Profile> current = _profileList.get(i);
                if (current.first.getSanitizedBaseURI().equals(item.first.getSanitizedBaseURI())) {
                    if (firstIndex < 0) {
                        firstIndex = i;
                    }
                    lastIndex = i;
                }
            }
            if (firstIndex >= 0) {
                _profileList.subList(firstIndex, lastIndex + 1).clear();
                notifyItemRangeRemoved(firstIndex, lastIndex - firstIndex + 1);
            }
        }
        _historyService.removeAllDataForInstance(item.first);
    }
}
