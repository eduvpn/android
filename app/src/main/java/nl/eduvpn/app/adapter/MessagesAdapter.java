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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import nl.eduvpn.app.R;
import nl.eduvpn.app.adapter.viewholder.MessageViewHolder;
import nl.eduvpn.app.entity.message.Maintenance;
import nl.eduvpn.app.entity.message.Message;
import nl.eduvpn.app.entity.message.Notification;
import nl.eduvpn.app.utils.FormattingUtils;

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
