package org.eduvpn.common;

public enum ServerType {
    Unknown(0),
    InstituteAccess(1),
    SecureInternet(2),
    Custom(3);
    public final Integer nativeValue;
    ServerType(Integer nativeValue) {
        this.nativeValue = nativeValue;
    }
}
