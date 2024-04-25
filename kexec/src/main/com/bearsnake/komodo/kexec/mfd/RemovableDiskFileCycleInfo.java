/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public class RemovableDiskFileCycleInfo extends DiskFileCycleInfo {

    private String _readKey;
    private String _writeKey;

    public final String getReadKey() { return _readKey; }
    public final String getWriteKey() { return _writeKey; }

    public final RemovableDiskFileCycleInfo setReadKey(final String value) {_readKey = value; return this; }
    public final RemovableDiskFileCycleInfo setWriteKey(final String value) {_writeKey = value; return this; }
}
