package org.eduvpn.common;

public enum Protocol {
    Unknown(0),
    OpenVPN(1),
    WireGuard(2),
    WireGuardWithTCP(3),
    OpenVPNWithTCP(100); // Using number 100 here because it has no mapping in common


    public final Integer nativeValue;
    Protocol(Integer nativeValue) {
        this.nativeValue = nativeValue;
    }
}
