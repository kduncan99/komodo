/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

public abstract class DeviceInfo {

    private final boolean _isMounted;
    private final boolean _isReady;
    private final boolean _isWriteProtected;

    public DeviceInfo(final boolean isMounted,
                      final boolean isReady,
                      final boolean isWriteProtected) {
        _isMounted = isMounted;
        _isReady = isReady;
        _isWriteProtected = isWriteProtected;
    }

    public final boolean isMounted() { return _isMounted; }
    public final boolean isReady() { return _isReady; }
    public final boolean isWriteProtected() { return _isWriteProtected; }

    @Override
    public String toString() {
        return String.format("mnt:%s rdy:%s wProt:%s", _isMounted, _isReady, _isWriteProtected);
    }
}
