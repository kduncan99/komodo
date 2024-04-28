/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public class FileFlags {

    private boolean _isLargeFile;
    private boolean _assignmentAcceleration;
    private boolean _isWrittenTo;
    private boolean _storeThrough;

    public int compose() {
        int value = _isLargeFile ? 040 : 0;
        value |= _assignmentAcceleration ? 004 : 0;
        value |= _isWrittenTo ? 002 : 0;
        value |= _storeThrough ? 001 : 0;
        return value;
    }

    public void extract(final int value) {
        _isLargeFile = (value & 040) != 0;
        _assignmentAcceleration = (value & 004) != 0;
        _isWrittenTo = (value & 002) != 0;
        _storeThrough = (value & 001) != 0;
    }

    public static FileFlags extractFrom(final int value) {
        var inf = new FileFlags();
        inf.extract(value);
        return inf;
    }

    public FileFlags setIsLargeFile(final boolean value) { _isLargeFile = value; return this; }
    public FileFlags setAssignmentAcceleration(final boolean value) { _assignmentAcceleration = value; return this; }
    public FileFlags setIsWrittenTo(final boolean value) { _isWrittenTo = value; return this; }
    public FileFlags setStoreThrough(final boolean value) { _storeThrough = value; return this; }
}
