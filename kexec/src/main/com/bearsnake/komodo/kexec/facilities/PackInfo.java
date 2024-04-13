/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.ArraySlice;

public class PackInfo implements MediaInfo {

    long       _directoryTrackId;
    boolean    _isFixed;
    boolean    _isPrepped;
    boolean    _isRemovable;
    ArraySlice _label;
    String     _packName;
    int        _prepFactor;
    long       _trackCount;

    public PackInfo() {}

    public PackInfo setDirectoryTrackId(long value) { _directoryTrackId = value; return this; }
    public PackInfo setIsFixed(boolean value) { _isFixed = value; return this; }
    public PackInfo setIsPrepped(boolean value) { _isPrepped = value; return this; }
    public PackInfo setIsRemovable(boolean value) { _isRemovable = value; return this; }
    public PackInfo setLabel(ArraySlice value) { _label = value; return this; }
    public PackInfo setPackName(String value) { _packName = value; return this; }
    public PackInfo setPrepFactor(int value) { _prepFactor = value; return this; }
    public PackInfo setTrackCount(long value) { _trackCount = value; return this; }

    @Override
    public String getMediaName() { return _packName; }
}
