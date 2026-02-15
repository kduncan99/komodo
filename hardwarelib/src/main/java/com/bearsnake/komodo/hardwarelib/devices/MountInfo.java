/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

/**
 * Additional information for mounting media on a device
 */
public class MountInfo {

    private final String _fileName;
    private final boolean _writeProtect;

    public MountInfo(
        final String fileName,
        final boolean writeProtected
    ) {
        _fileName = fileName;
        _writeProtect = writeProtected;
    }

    public String getFileName() {
        return _fileName;
    }

    public boolean getWriteProtected() {
        return _writeProtect;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("[").append(_fileName);
        if (_writeProtect) {
            sb.append(" protected");
        }
        sb.append("]");
        return sb.toString();
    }
}
