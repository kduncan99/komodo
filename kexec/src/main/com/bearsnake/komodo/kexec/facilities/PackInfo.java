/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.exec.Exec;

public class PackInfo implements MediaInfo {

    private long       _directoryTrackAddress;
    private boolean    _isFixed;
    private boolean    _isPrepped;
    private boolean    _isRemovable;
    private int        _ldatIndex;
    private String     _packName;
    private int        _prepFactor;
    private long       _trackCount;

    public PackInfo() {}

    public long getDirectoryTrackAddress() { return _directoryTrackAddress; }
    public String getPackName() { return _packName; }
    public int getPrepFactor() { return _prepFactor; }
    public long getTrackCount() { return _trackCount; }
    public int getLDATIndex() { return _ldatIndex; }
    public boolean isFixed() { return _isFixed; }
    public boolean isPrepped() { return _isPrepped; }
    public boolean isRemovable() { return _isRemovable; }

    public PackInfo setDirectoryTrackAddress(final long value) { _directoryTrackAddress = value; return this; }
    public PackInfo setIsFixed(final boolean value) { _isFixed = value; return this; }
    public PackInfo setIsPrepped(final boolean value) { _isPrepped = value; return this; }
    public PackInfo setIsRemovable(final boolean value) { _isRemovable = value; return this; }
    public PackInfo setLDATIndex(final int value) { _ldatIndex = value; return this; }
    public PackInfo setPackName(final String value) { _packName = value; return this; }
    public PackInfo setPrepFactor(final int value) { _prepFactor = value; return this; }
    public PackInfo setTrackCount(final long value) { _trackCount = value; return this; }

    @Override
    public String getMediaName() { return _packName; }

    public static PackInfo loadFromLabel(
        final ArraySlice label,
        final ArraySlice initialDirectoryTrack
    ) {
        if (!Word36.toStringFromASCII(label._array[0]).equals("VOL1")) {
            return null;
        }

        var pi = new PackInfo();
        var packName = Word36.toStringFromASCII(label._array[01]) + Word36.toStringFromASCII(label._array[02]);
        pi._packName = packName.substring(0, 6).trim();
        pi._prepFactor = (int)Word36.getH2(label._array[04]);
        pi._isPrepped = Exec.isValidPrepFactor(pi._prepFactor) && Exec.isValidPackName(pi._packName);
        pi._directoryTrackAddress = label._array[03];
        pi._trackCount = label._array[016];

        if (pi._isPrepped) {
            if (Word36.getH1(initialDirectoryTrack._array[05]) == 0) {
                pi._isFixed = false;
                pi._isRemovable = true;
                pi._ldatIndex = (int) Word36.getH1(initialDirectoryTrack._array[020]);
            } else {
                pi._isFixed = true;
                pi._isRemovable = false;
                pi._ldatIndex = (int) Word36.getH1(initialDirectoryTrack._array[05]) & 07777;
            }
        } else {
            pi._isFixed = false;
            pi._isRemovable = false;
            pi._ldatIndex = 0;
        }

        return pi;
    }
}
