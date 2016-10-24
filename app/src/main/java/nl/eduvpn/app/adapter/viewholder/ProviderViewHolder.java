package nl.eduvpn.app.adapter.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import nl.eduvpn.app.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Viewholder for the provider instance list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class ProviderViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.provider_icon)
    public ImageView providerIcon;

    @BindView(R.id.provider_display_name)
    public TextView providerDisplayName;

    public ProviderViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
