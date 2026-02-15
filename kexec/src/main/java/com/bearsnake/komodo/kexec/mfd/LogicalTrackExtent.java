/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public class LogicalTrackExtent {

    private long _trackId;
    private long _trackCount;

    public LogicalTrackExtent(final long trackId,
                              final long trackCount) {
        _trackId = trackId;
        _trackCount = trackCount;
    }

    public void addToTrackCount(long count) { _trackCount += count; }
    public void addToTrackId(long count) { _trackId += count; }
    public long getHighestTrack() { return _trackId + _trackCount - 1; }
    public long getTrackId() { return _trackId; }
    public long getTrackCount() { return _trackCount; }

    /**
     * Returns true if this object is logically contiguous and follows the
     * comparison object
     * @param previous potentially-previous contiguous entity
     */
    public boolean isContiguousTo(final LogicalTrackExtent previous) {
        return previous._trackId + previous._trackCount == _trackId;
    }

    public void setTrackCount(long trackCount) { _trackCount = trackCount; }
    public void setTrackId(long trackId) { _trackId = trackId; }

    @Override
    public int hashCode() {
        return (int)_trackId;
    }

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof LogicalTrackExtent tr)
            && (_trackId == tr._trackId) && (_trackCount == tr._trackCount);
    }

    @Override
    public String toString() {
        return String.format("[Id:%04o Cnt:%06o]", _trackId, _trackCount);
    }
}
