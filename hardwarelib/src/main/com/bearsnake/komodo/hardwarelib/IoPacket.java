/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

public abstract class IoPacket {

    public static class MountInfo {

        private final String _fileName;
        private final boolean _writeProtect;

        public MountInfo(
            final String fileName,
            final boolean writeProtected
        ) {
            _fileName = fileName;
            _writeProtect = writeProtected;
        }

        public String getFileName() { return _fileName; }
        public boolean getWriteProtected() { return _writeProtect; }
    }

    private IoFunction _function;
    private IoListener _listener;
    private IoStatus _status;

    public IoFunction getFunction() { return _function; }
    public IoListener getListener() { return _listener; }
    public IoStatus getStatus() { return _status; }
    public IoPacket setFunction(final IoFunction function) { _function = function; return this; }
    public IoPacket setListener(final IoListener listener) { _listener = listener; return this; }
    public IoPacket setStatus(final IoStatus status) { _status = status; return this; }

    @Override
    public String toString() {
        return String.format("%s", _function);
    }
}
