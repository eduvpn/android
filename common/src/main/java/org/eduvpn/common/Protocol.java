package org.eduvpn.common;

public enum Protocol {
    Unknown(0),
    OpenVPN(1),
    WireGuard(2);

    public final Integer nativeValue;
    Protocol(Integer nativeValue) {
        this.nativeValue = nativeValue;
    }
}
