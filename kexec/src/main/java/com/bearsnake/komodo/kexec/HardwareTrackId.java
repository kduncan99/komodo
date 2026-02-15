/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

public class HardwareTrackId {

    private int _ldatIndex;
    private long _trackId;

    public HardwareTrackId(final int ldatIndex,
                           final long trackId) {
        _ldatIndex = ldatIndex;
        _trackId = trackId;
    }

    public void addToTrackId(final long count) { _trackId += count; }
    public int  getLDATIndex() { return _ldatIndex; }
    public long getTrackId() { return _trackId; }
    public void setLDATIndex(final int value) { _ldatIndex = value; }
    public void setTrackId(final long value) { _trackId = value; }

    /**
     * Returns true if this track ID follows the previous track ID, by the given track count
     * @param previous previous track ID
     * @param previousTrackCount number of tracks which exist beginning with the previous track ID,
     *                           and leading up to, but not including, our track ID.
     */
    public boolean isContiguousTo(final HardwareTrackId previous,
                                  final long previousTrackCount) {
        return (_ldatIndex == previous._ldatIndex)
            && (_trackId == previous._trackId + previousTrackCount);
    }

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
