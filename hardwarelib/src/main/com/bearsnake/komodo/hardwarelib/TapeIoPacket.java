/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import java.nio.ByteBuffer;

public class TapeIoPacket extends IoPacket {

    public static class Info {

        private final boolean _isMounted;
        private final boolean _isReady;
        private final boolean _isWriteProtected;

        public Info(final boolean isMounted,
                    final boolean isReady,
                    final boolean isWriteProtected) {
            _isMounted = isMounted;
            _isReady = isReady;
            _isWriteProtected = isWriteProtected;
        }

        public boolean isMounted() { return _isMounted; }
        public boolean isReady() { return _isReady; }
        public boolean isWriteProtected() { return _isWriteProtected; }

        public static Info deserialize(final ByteBuffer bb) {
            var isMounted = bb.getInt() != 0;
            var isReady = bb.getInt() != 0;
            var isWriteProtected = bb.getInt() != 0;
            return new Info(isMounted, isReady, isWriteProtected);
        }

        public void serialize(final ByteBuffer bb) {
            bb.putInt(_isMounted ? 1 : 0);
            bb.putInt(_isReady ? 1 : 0);
            bb.putInt(_isWriteProtected ? 1 : 0);
        }
    }

    public ByteBuffer _buffer;
    public int _bytesTransferred;
    public MountInfo _mountInfo;

    public ByteBuffer getBuffer() { return _buffer; }
    public int getBytesTransferred() { return _bytesTransferred; }
    public MountInfo getMountInfo() { return _mountInfo; }
    public TapeIoPacket setBuffer(final ByteBuffer buffer) { _buffer = buffer; return this; }
    public TapeIoPacket setBytesTransferred(final int value) { _bytesTransferred = value; return this; }
    public TapeIoPacket setMountInfo(final MountInfo mountInfo) { _mountInfo = mountInfo; return this; }
}
