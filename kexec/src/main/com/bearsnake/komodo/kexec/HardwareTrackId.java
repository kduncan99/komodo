/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

public class HardwareTrackId {

    private long _ldatIndex;
    private long _trackId;

    public HardwareTrackId(final long ldatIndex,
                           final long trackId) {
        _ldatIndex = ldatIndex;
        _trackId = trackId;
    }

    public void addToTrackId(long count) { _trackId += count; }
    public long getLDATIndex() { return _ldatIndex; }
    public long getTrackId() { return _trackId; }
    public void setLDATIndex(long value) { _ldatIndex = value; }
    public void setTrackId(long value) { _trackId = value; }

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof HardwareTrackId ht)
            && (_ldatIndex == ht._ldatIndex) && (_trackId == ht._trackId);
    }

    @Override
    public int hashCode() {
        return (int)_trackId;
    }

    @Override
    public String toString() {
        return String.format("[LDAT:%04o TId:%06o]", _ldatIndex, _trackId);
    }
}
