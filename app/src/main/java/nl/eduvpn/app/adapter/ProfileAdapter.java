package nl.eduvpn.app.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import nl.eduvpn.app.R;
import nl.eduvpn.app.adapter.viewholder.ProfileViewHolder;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.Profile;
import nl.eduvpn.app.utils.FormattingUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the profile list.
 * Created by Daniel Zolnai on 2016-10-11.
 */
public class ProfileAdapter extends RecyclerView.Adapter<ProfileViewHolder> {

    private List<Pair<Instance, Profile>> _profileList;
    private LayoutInflater _layoutInflater;

    /**
     * Constructor.
     *
     * @param profileList The list of instance and profile pairs to put in the list.
     */
    public ProfileAdapter(@Nullable List<Pair<Instance, Profile>> profileList) {
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
    public synchronized void addItems(List<Pair<Instance, Profile>> profiles) {
        _profileList.addAll(profiles);
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
        Pair<Instance, Profile> instanceProfilePair = getItem(position);
        holder.profileName.setText(
                FormattingUtils.formatProfileName(
                        holder.profileName.getContext(),
                        instanceProfilePair.first,
                        instanceProfilePair.second));
        if (instanceProfilePair.first.getLogoUri() != null) {
            Picasso.with(holder.providerIcon.getContext())
                    .load(instanceProfilePair.first.getLogoUri())
                    .placeholder(R.drawable.vpn_icon)
                    .fit()
                    .noFade()
                    .into(holder.providerIcon);
        } else {
            holder.providerIcon.setImageResource(R.drawable.vpn_icon);
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
        return _profileList != null ? _profileList.size() : 0;
    }
}
