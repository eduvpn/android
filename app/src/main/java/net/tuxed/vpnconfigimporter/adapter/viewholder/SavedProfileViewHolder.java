package net.tuxed.vpnconfigimporter.adapter.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.tuxed.vpnconfigimporter.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Viewholder for the provider instance list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class SavedProfileViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.providerIcon)
    public ImageView providerIcon;

    @BindView(R.id.displayName)
    public TextView displayName;

    public SavedProfileViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
