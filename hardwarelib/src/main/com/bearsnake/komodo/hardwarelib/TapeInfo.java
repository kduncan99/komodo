/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import java.nio.ByteBuffer;

public class TapeInfo {

    private final boolean _isMounted;
    private final boolean _isReady;
    private final boolean _isWriteProtected;

    public TapeInfo(final boolean isMounted,
                    final boolean isReady,
                    final boolean isWriteProtected) {
        _isMounted = isMounted;
        _isReady = isReady;
        _isWriteProtected = isWriteProtected;
    }

    public boolean isMounted() {
        return _isMounted;
    }

    public boolean isReady() {
        return _isReady;
    }

    public boolean isWriteProtected() {
        return _isWriteProtected;
    }

    public static TapeInfo deserialize(final ByteBuffer bb) {
        var isMounted = bb.getInt() != 0;
        var isReady = bb.getInt() != 0;
        var isWriteProtected = bb.getInt() != 0;
        return new TapeInfo(isMounted, isReady, isWriteProtected);
    }

    public void serialize(final ByteBuffer bb) {
        bb.putInt(_isMounted ? 1 : 0);
        bb.putInt(_isReady ? 1 : 0);
        bb.putInt(_isWriteProtected ? 1 : 0);
    }

    @Override
    public String toString() {
        return String.format("[mnt:%s rdy:%s wProt:%s]",
                             _isMounted,
                             _isReady,
                             _isWriteProtected);
    }
}
