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
