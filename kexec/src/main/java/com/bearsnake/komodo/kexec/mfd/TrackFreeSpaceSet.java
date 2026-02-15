/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import java.io.PrintStream;
import java.util.LinkedList;

/**
 * Manages allocations of tracks in the context of a disk pack.
 */
public class TrackFreeSpaceSet {

    private final LinkedList<PackTrackRegion> _freeSpace = new LinkedList<>();
    private final long _trackCount;
    private long _availableTrackCount;

    public TrackFreeSpaceSet(
        final long trackCount
    ) {
        _freeSpace.add(new PackTrackRegion(0, trackCount));
        _trackCount = trackCount;
        _availableTrackCount = trackCount;
    }

    public long getAvailableTrackCount() { return _availableTrackCount; }
    public long getTrackCount() { return _trackCount; }

    /**
     * Allocates a single track. Attempts to find a free-space region of exactly one track.
     * If no such region exists, we carve the first track from the first free-space region.
     * @return track id of allocated track unless there is no free space, in which case we return null.
     */
    public synchronized Long allocateTrack() {
        if (_freeSpace.isEmpty()) {
            return null;
        }

        for (var tr : _freeSpace) {
            if (tr.getTrackCount() == 1) {
                _freeSpace.remove(tr);
                _availableTrackCount--;
                return tr.getTrackId();
            }
        }

        var tr = _freeSpace.getFirst();
        var result = tr.getTrackId();
        tr.adjustTrackId(1);
        tr.adjustTrackCount(-1);
        _availableTrackCount--;
        return result;
    }

    public void dump(final PrintStream out,
                     final String indent) {
        out.printf("%sTrack Count=%d Available=%d\n", indent, _trackCount, _availableTrackCount);
        for (var tr : _freeSpace) {
            out.printf("%s  Addr:%08o Count:%d\n", indent, tr.getTrackId(), tr.getTrackCount());
        }
    }

    // TODO
    //   markUnallocated()
    //      StopCode.TrackToBeReleasedWasNotAllocated

    /**
     * Marks a particular region of the free space set as allocated
     * (by removing the region from the free space list)
     * @param trackId id of the first track in the region to be allocated
     * @param trackCount number of tracks to be allocated
     * @return true if successful, false if caller tried to allocate space which is already allocated, or out of range.
     */
    public synchronized boolean markAllocated(
        final long trackId,
        final long trackCount
    ) {
        // sanity checks
        if (trackCount == 0) {
            // odd, but not an error
            return true;
        }

        if ((trackId < 0) || (trackCount < 0)) {
            return false;
        }

        final long highest = trackId + trackCount - 1;
        if (highest >= _trackCount) {
            return false;
        }

        for (int fsx = 0; fsx < _freeSpace.size(); fsx++) {
            var tr = _freeSpace.get(fsx);
            if (tr.getHighestTrackId() >= trackId) {
                if (tr.getTrackId() > highest) {
                    // The previous entry did not contain the requested space,
                    // and this entry's first track is beyond the requested space.
                    // Caller is requesting already-allocated space - error.
                    return false;
                }

                if (tr.getTrackId() > trackId) {
                    // The requested entry describes space in this entry, but also space
                    // ahead of this entry. Some of the space is already allocated - error.
                    return false;
                }

                // At this point, the request is validated.
                if (tr.getTrackId() == trackId) {
                    // requested region is aligned with the front of this entry.
                    if (tr.getTrackCount() == trackCount) {
                        // requested region is exactly this region - just remove it.
                        _freeSpace.remove(fsx);
                        _availableTrackCount -= trackCount;
                        return true;
                    }

                    // remove requested region from the entry.
                    tr.adjustTrackId(trackCount);
                    tr.adjustTrackCount(-trackCount);
                    _availableTrackCount -= trackCount;
                    return true;
                }

                if (tr.getHighestTrackId() == highest) {
                    // requested region is aligned with the back of this entry.
                    // it is a subset, so just remove the requested region from the entry.
                    tr.adjustTrackCount(-trackCount);
                    _availableTrackCount -= trackCount;
                    return true;
                }

                // The requested region is a subset of this entry, but not aligned with either
                // the front or the back - we need to create a new entry as well as updating this entry.
                var newTr = new PackTrackRegion(trackId + trackCount, tr.getHighestTrackId() - highest);
                tr.setTrackCount(trackId - tr.getTrackId());
                _freeSpace.add(fsx + 1, newTr);
                _availableTrackCount -= trackCount;
                return true;
            }
        }

        // If we get here, something is very wrong.
        return false;
    }

    /**
     * Resets the table such that the entire space is considered free.
     * This must be followed (probably by mfd) by marking the track with VOL1 label, and the first directory track,
     * as allocated.
     */
    public void reset() {
        _freeSpace.clear();
        _freeSpace.add(new PackTrackRegion(0, _trackCount));
        _availableTrackCount = _trackCount;
    }
}
