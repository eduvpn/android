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

import android.support.annotation.NonNull;

import java.util.List;

/**
 * The application configuration.
 * Created by Daniel Zolnai on 2016-10-07.
 */
public class InstanceList {

    private Integer _version;
    private List<Instance> _instanceList;

    public InstanceList(@NonNull Integer version, @NonNull List<Instance> instanceList) {
        _version = version;
        _instanceList = instanceList;
    }

    @NonNull
    public Integer getVersion() {
        return _version;
    }

    @NonNull
    public List<Instance> getInstanceList() {
        return _instanceList;
    }
}
