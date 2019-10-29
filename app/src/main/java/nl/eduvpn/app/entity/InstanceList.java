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

package nl.eduvpn.app.entity;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * The application configuration.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class InstanceList {

    private Integer _sequenceNumber;
    private List<Instance> _instanceList;

    public InstanceList(@NonNull List<Instance> instanceList, @NonNull Integer sequenceNumber) {
        _instanceList = instanceList;
        _sequenceNumber = sequenceNumber;
    }

    @NonNull
    public List<Instance> getInstanceList() {
        return _instanceList;
    }

    @NonNull
    public Integer getSequenceNumber() {
        return _sequenceNumber;
    }
}
