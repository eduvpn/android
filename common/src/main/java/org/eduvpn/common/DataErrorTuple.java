package org.eduvpn.common;

import androidx.annotation.Nullable;

public class DataErrorTuple {
    public final @Nullable String data;
    public final @Nullable String error;

    DataErrorTuple(@Nullable String data, @Nullable String error) {
        this.data = data;
        this.error = error;
    }
    public boolean isError() {
        return error != null;
    }
}
