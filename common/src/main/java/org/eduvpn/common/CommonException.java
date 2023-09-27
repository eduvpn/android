package org.eduvpn.common;

public class CommonException extends Exception {
    public CommonException(String nativeError) {
        super("Error in common Go library: " + nativeError);
    }
}
