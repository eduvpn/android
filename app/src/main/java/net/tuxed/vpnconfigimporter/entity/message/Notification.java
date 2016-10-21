package net.tuxed.vpnconfigimporter.entity.message;

import java.util.Date;

/**
 * A general notification message.
 * Created by Daniel Zolnai on 2016-10-19.
 */
public class Notification extends Message {

    private String _content;

    public Notification(Date date, String content) {
        super(date);
        _content = content;
    }

    public String getContent() {
        return _content;
    }
}
