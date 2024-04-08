/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

public class TrackRegion {

    private long _trackId;
    private long _trackCount;

    public TrackRegion(final long trackId,
                       final long trackCount) {
        _trackId = trackId;
        _trackCount = trackCount;
    }

    public void addToTrackCount(long count) { _trackCount += count; }
    public void addToTrackId(long count) { _trackId += count; }
    public long getHighestTrack() { return _trackId + _trackCount - 1; }
    public long getTrackId() { return _trackId; }
    public long getTrackCount() { return _trackCount; }
    public void setTrackCount(long trackCount) { _trackCount = trackCount; }
    public void setTrackId(long trackId) { _trackId = trackId; }

    @Override
    public int hashCode() {
        return (int)_trackId;
    }

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof TrackRegion tr)
            && (_trackId == tr._trackId) && (_trackCount == tr._trackCount);
    }

    @Override
    public String toString() {
        return String.format("[Id:%04o Cnt:%06o]", _trackId, _trackCount);
    }
}
