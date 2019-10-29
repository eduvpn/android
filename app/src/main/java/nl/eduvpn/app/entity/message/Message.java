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

package nl.eduvpn.app.entity.message;

import java.util.Date;

import androidx.annotation.NonNull;

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
