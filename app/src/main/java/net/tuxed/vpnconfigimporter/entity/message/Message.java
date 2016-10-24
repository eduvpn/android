package net.tuxed.vpnconfigimporter.entity.message;

import android.support.annotation.NonNull;

import java.util.Date;

/**
 * Describes a message object.
 * Created by Daniel Zolnai on 2016-10-19.
 */
public abstract class Message implements Comparable<Message> {

    private Date _date;

    public Message(Date date) {
        _date = date;
    }

    public Date getDate() {
        return _date;
    }

    @Override
    public int compareTo(@NonNull Message other) {
        // Idea is that more recent messages show up at the top
        final int BEFORE = -1;
        final int AFTER = 1;
        if (other._date == null) {
            return AFTER;
        } else if (_date == null) {
            return BEFORE;
        } else {
            return -_date.compareTo(other._date);
        }
    }
}
