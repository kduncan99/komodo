/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

/**
 * Describes a region of a disk pack in terms of the id of the first track in the region
 * and the number of tracks in the region.
 */
public class PackTrackRegion {

    private long _trackId;
    private long _trackCount;

    public PackTrackRegion(
        final long trackId,
        final long trackCount
    ) {
        _trackId = trackId;
        _trackCount = trackCount;
    }

    public PackTrackRegion adjustTrackCount(final long delta) {_trackCount += delta; return this; }
    public PackTrackRegion adjustTrackId(final long delta) {_trackId += delta; return this; }
    public long getHighestTrackId() { return _trackId + _trackCount - 1; }
    public long getTrackCount() { return _trackCount; }
    public long getTrackId() { return _trackId; }
    public PackTrackRegion setTrackCount(final long value) {_trackCount = value; return this; }
    public PackTrackRegion setTrackId(final long value) {_trackId = value; return this; }
}
