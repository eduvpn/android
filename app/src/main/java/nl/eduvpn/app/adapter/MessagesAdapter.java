package net.tuxed.vpnconfigimporter.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.tuxed.vpnconfigimporter.R;
import net.tuxed.vpnconfigimporter.adapter.viewholder.MessageViewHolder;
import net.tuxed.vpnconfigimporter.entity.message.Maintenance;
import net.tuxed.vpnconfigimporter.entity.message.Message;
import net.tuxed.vpnconfigimporter.entity.message.Notification;
import net.tuxed.vpnconfigimporter.utils.FormattingUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter for serving the message views inside a list.
 * Created by Daniel Zolnai on 2016-10-19.
 */
public class MessagesAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private List<Message> _userMessages;
    private List<Message> _systemMessages;

    private List<Message> _mergedList = new ArrayList<>();

    private LayoutInflater _layoutInflater;

    public void setUserMessages(List<Message> userMessages) {
        _userMessages = userMessages;
        _regenerateList();
    }

    public void setSystemMessages(List<Message> systemMessages) {
        _systemMessages = systemMessages;
        _regenerateList();
    }

    private void _regenerateList() {
        _mergedList.clear();
        if (_userMessages != null) {
            _mergedList.addAll(_userMessages);
        }
        if (_systemMessages != null) {
            _mergedList.addAll(_systemMessages);
        }
        Collections.sort(_mergedList);
        notifyDataSetChanged();
    }


    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (_layoutInflater == null) {
            _layoutInflater = LayoutInflater.from(parent.getContext());
        }
        return new MessageViewHolder(_layoutInflater.inflate(R.layout.list_item_message, parent, false));
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = _mergedList.get(position);
        if (message instanceof Maintenance) {
            holder.messageIcon.setVisibility(View.VISIBLE);
            Context context = holder.messageText.getContext();
            String maintenanceText = FormattingUtils.getMaintenanceText(context, (Maintenance)message);
            holder.messageText.setText(maintenanceText);
        } else if (message instanceof Notification) {
            holder.messageIcon.setVisibility(View.GONE);
            holder.messageText.setText(((Notification)message).getContent());
        } else {
            throw new RuntimeException("Unexpected message type!");
        }

    }

    @Override
    public int getItemCount() {
        return _mergedList.size();
    }
}
