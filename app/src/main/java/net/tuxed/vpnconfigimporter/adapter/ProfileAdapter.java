package net.tuxed.vpnconfigimporter.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.adapter.viewholder.ProfileViewHolder;
import net.tuxed.vpnconfigimporter.adapter.viewholder.ProviderViewHolder;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.service.ConfigurationService;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Adapter for the profile list.
 * Created by Daniel Zolnai on 2016-10-11.
 */
public class ProfileAdapter extends RecyclerView.Adapter<ProfileViewHolder> {

    private Instance _instance;
    private List<Profile> _profileList;
    private LayoutInflater _layoutInflater;

    /**
     * Constructor.
     *
     * @param instance The instance the profiles are for.
     */
    public ProfileAdapter(Instance instance) {
        _instance = instance;
    }

    /**
     * Changes the items of this adapter.
     *
     * @param profiles The list of profiles to set.
     */
    public void setItems(List<Profile> profiles) {
        _profileList = profiles;
        notifyDataSetChanged();
    }

    /**
     * Returns the item at the given position.
     *
     * @param position The position of the item.
     * @return The item at the given position.
     */
    public Profile getItem(int position) {
        return _profileList.get(position);
    }


    @Override
    public ProfileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (_layoutInflater == null) {
            _layoutInflater = LayoutInflater.from(parent.getContext());
        }
        View view = _layoutInflater.inflate(R.layout.list_item_config, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ProfileViewHolder holder, int position) {
        Profile profile = getItem(position);
        holder.profileName.setText(profile.getDisplayName());
        if (_instance.getLogoUri() != null) {
            Picasso.with(holder.providerIcon.getContext())
                    .load(_instance.getLogoUri())
                    .placeholder(R.drawable.vpn_icon)
                    .fit()
                    .into(holder.providerIcon);
        } else {
            holder.providerIcon.setImageResource(R.drawable.vpn_icon);
        }
    }

    @Override
    public int getItemCount() {
        return _profileList != null ? _profileList.size() : 0;
    }
}
