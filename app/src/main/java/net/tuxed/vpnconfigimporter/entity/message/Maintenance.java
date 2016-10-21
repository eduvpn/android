package net.tuxed.vpnconfigimporter.entity.message;

import java.util.Date;

/**
 * A maintenance message.
 * Created by Daniel Zolnai on 2016-10-19.
 */
public class Maintenance extends Message {

    private Date _start;
    private Date _end;

    public Maintenance(Date date, Date start, Date end) {
        super(date);
        _start = start;
        _end = end;
    }

    public Date getStart() {
        return _start;
    }

    public Date getEnd() {
        return _end;
    }
}
