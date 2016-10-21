package net.tuxed.vpnconfigimporter.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.adapter.viewholder.ProviderViewHolder;
import net.tuxed.vpnconfigimporter.adapter.viewholder.SavedProfileViewHolder;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.SavedProfile;
import net.tuxed.vpnconfigimporter.service.ConfigurationService;
import net.tuxed.vpnconfigimporter.utils.FormattingUtils;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Adapter for the saved profiles list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class SavedProfileAdapter extends RecyclerView.Adapter<SavedProfileViewHolder> {

    private List<SavedProfile> _savedProfileList;
    private LayoutInflater _layoutInflater;

    public SavedProfileAdapter(List<SavedProfile> savedProfileList) {
        _savedProfileList = savedProfileList;
    }

    /**
     * Returns the item at the given position.
     *
     * @param position The position of the item.
     * @return The item at the given position.
     */
    public SavedProfile getItem(int position) {
        return _savedProfileList.get(position);
    }


    @Override
    public SavedProfileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (_layoutInflater == null) {
            _layoutInflater = LayoutInflater.from(parent.getContext());
        }
        View view = _layoutInflater.inflate(R.layout.list_item_saved_profile, parent, false);
        return new SavedProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SavedProfileViewHolder holder, int position) {
        SavedProfile savedProfile = getItem(position);
        holder.displayName.setText(FormattingUtils.formatSavedProfileName(holder.displayName.getContext(), savedProfile));

        if (savedProfile.getInstance().getLogoUri() != null) {
            Picasso.with(holder.providerIcon.getContext())
                    .load(savedProfile.getInstance().getLogoUri())
                    .placeholder(R.drawable.vpn_icon)
                    .fit()
                    .into(holder.providerIcon);
        } else {
            holder.providerIcon.setImageResource(R.drawable.vpn_icon);
        }
    }

    @Override
    public int getItemCount() {
        return _savedProfileList == null ? 0 : _savedProfileList.size();
    }
}
