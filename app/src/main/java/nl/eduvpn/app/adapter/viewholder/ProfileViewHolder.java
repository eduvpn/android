package nl.eduvpn.app.adapter.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import nl.eduvpn.app.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * View holder for the provider instance list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class ProfileViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.providerIcon)
    public ImageView providerIcon;

    @BindView(R.id.profileName)
    public TextView profileName;

    @BindView(R.id.undoButton)
    public Button undoButton;

    public ProfileViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
