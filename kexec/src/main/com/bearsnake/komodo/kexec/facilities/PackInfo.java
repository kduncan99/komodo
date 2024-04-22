/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.ArraySlice;

import javax.print.DocFlavor;

public class PackInfo implements MediaInfo {

    private long       _directoryTrackId;
    private boolean    _isFixed;
    private boolean    _isPrepped;
    private boolean    _isRemovable;
    private ArraySlice _label;
    private String     _packName;
    private int        _prepFactor;
    private long       _trackCount;

    public PackInfo() {}

    public long getDirectoryTrackId() { return _directoryTrackId; }
    public ArraySlice getLabel() { return _label; }
    public String getPackName() { return _packName; }
    public int getPrepFactor() { return _prepFactor; }
    public long getTrackCount() { return _trackCount; }
    public boolean isFixed() { return _isFixed; }
    public boolean isPrepped() { return _isPrepped; }
    public boolean isRemovable() { return _isRemovable; }
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
