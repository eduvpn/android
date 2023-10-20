package org.eduvpn.common;

import androidx.annotation.Nullable;

public class FailoverResult {
    public final boolean doesRequireFailover;
    public final @Nullable String error;

    FailoverResult(boolean doesRequireFailover, @Nullable String error) {
        this.doesRequireFailover = doesRequireFailover;
        this.error = error;
    }
    public boolean isError() {
        return error != null;
    }
}
