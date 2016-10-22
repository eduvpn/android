package net.tuxed.vpnconfigimporter.adapter.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.tuxed.vpnconfigimporter.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * View holder for the notification item view.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class MessageViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.messageText)
    public TextView messageText;

    @BindView(R.id.messageIcon)
    public ImageView messageIcon;

    public MessageViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
