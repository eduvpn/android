package org.eduvpn.common;

import androidx.annotation.Nullable;

public class StateCB {
    public final int oldState;
    public final int newState;
    @Nullable
    public final Object data;

    public StateCB(int oldState, int newState, @Nullable Object data) {
        this.oldState = oldState;
        this.newState = newState;
        this.data = data;
    }
}
