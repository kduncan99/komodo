/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.mfd.MFDRelativeAddress;
import com.bearsnake.komodo.kexec.mfd.TrackFreeSpaceSet;

import java.io.PrintStream;
import java.util.HashMap;

/**
 * Describes a disk pack
 */
public class PackInfo implements MediaInfo {

    private static HashMap<Integer, Long> BLOCK_ALIGNMENT_MASK = new HashMap<>();
    static {
        BLOCK_ALIGNMENT_MASK.put(28, 0xFFFFFFFFFFFFFFFFL);
        BLOCK_ALIGNMENT_MASK.put(56, 0xFFFFFFFFFFFFFFFEL);
        BLOCK_ALIGNMENT_MASK.put(112, 0xFFFFFFFFFFFFFFFCL);
        BLOCK_ALIGNMENT_MASK.put(224, 0xFFFFFFFFFFFFFFF8L);
        BLOCK_ALIGNMENT_MASK.put(448, 0xFFFFFFFFFFFFFFF0L);
        BLOCK_ALIGNMENT_MASK.put(896, 0xFFFFFFFFFFFFFFE0L);
        BLOCK_ALIGNMENT_MASK.put(1792, 0xFFFFFFFFFFFFFFC0L);
    }

    private long              _directoryTrackAddress; // DRWA (word address)
    private boolean           _isFixed;
    private boolean           _isPrepped;
    private boolean           _isRemovable;
    private int               _ldatIndex;
    private String            _packName;
    private int               _prepFactor;
    private long              _trackCount;
    private int               _mfdTrackCount;
    private TrackFreeSpaceSet _freeSpace;

    public PackInfo() {}

    public MFDRelativeAddress alignSectorAddressToBlock(final MFDRelativeAddress sectorAddress) {
        var addr = sectorAddress.getValue() & BLOCK_ALIGNMENT_MASK.get(_prepFactor);
        return new MFDRelativeAddress(addr);
    }

    public long getDirectoryTrackAddress() { return _directoryTrackAddress; }
    public TrackFreeSpaceSet getFreeSpace() { return _freeSpace; }
    public int getLDATIndex() { return _ldatIndex; }
    public int getMFDTrackCount() { return _mfdTrackCount; }
    public String getPackName() { return _packName; }
    public int getPrepFactor() { return _prepFactor; }
    public long getTrackCount() { return _trackCount; }
    public boolean isFixed() { return _isFixed; }
    public boolean isPrepped() { return _isPrepped; }
    public boolean isRemovable() { return _isRemovable; }

    public PackInfo setDirectoryTrackAddress(final long value) { _directoryTrackAddress = value; return this; }
    public PackInfo setIsFixed(final boolean value) { _isFixed = value; return this; }
    public PackInfo setIsPrepped(final boolean value) { _isPrepped = value; return this; }
    public PackInfo setIsRemovable(final boolean value) { _isRemovable = value; return this; }
    public PackInfo setLDATIndex(final int value) { _ldatIndex = value; return this; }
    public PackInfo setMFDTrackCount(final int value) { _mfdTrackCount = value; return this; }
    public PackInfo setPackName(final String value) { _packName = value; return this; }
    public PackInfo setPrepFactor(final int value) { _prepFactor = value; return this; }

    public PackInfo setTrackCount(final long value) {
        _freeSpace = new TrackFreeSpaceSet(value);
        _trackCount = value;
        return this;
    }

    @Override
    public String getMediaName() { return _packName; }

    public static PackInfo loadFromLabel(
        final ArraySlice label,
        final ArraySlice initialDirectoryTrack
    ) {
        if (!Word36.toStringFromASCII(label.get(0)).equals("VOL1")) {
            return null;
        }

        var pi = new PackInfo();
        var packName = Word36.toStringFromASCII(label.get(01)) + Word36.toStringFromASCII(label.get(02));
        pi._packName = packName.substring(0, 6).trim();
        pi._prepFactor = (int)Word36.getH2(label.get(04));
        pi._isPrepped = Exec.isValidPrepFactor(pi._prepFactor) && Exec.isValidPackName(pi._packName);
        pi._directoryTrackAddress = label.get(03);
        pi._trackCount = label._array[016];

        var sector1 = new ArraySlice(initialDirectoryTrack, 28, 28);

        if (pi._isPrepped) {
            if (Word36.getH1(sector1.get(05)) == 0) {
                pi._isFixed = false;
                pi._isRemovable = true;
                pi._ldatIndex = (int) Word36.getH1(sector1.get(020));
            } else {
                pi._isFixed = true;
                pi._isRemovable = false;
                pi._ldatIndex = (int) Word36.getH1(sector1.get(05)) & 07777;
            }
        } else {
            pi._isFixed = false;
            pi._isRemovable = false;
            pi._ldatIndex = 0;
        }

        return pi;
    }

    @Override
    public void dump(final PrintStream out,
                     final String indent) {
        var sb = new StringBuilder().append(indent).append("Pack:").append(_packName);
        if (_isPrepped) {
            sb.append(" PREPPED RECL:").append(_prepFactor);
            if (_isFixed) {
                sb.append(" FIX");
            } else if (_isRemovable) {
                sb.append(" REM");
            }
        }

        sb.append(" LDAT:").append(String.format("%06o", _ldatIndex));
        sb.append(" DirTrk:").append(String.format("%06o", _directoryTrackAddress));
        sb.append(" Tracks:").append(_trackCount).append(" MFDTrks:").append(_mfdTrackCount);
        out.println(sb);
        _freeSpace.dump(out, indent + "  ");
    }
}
