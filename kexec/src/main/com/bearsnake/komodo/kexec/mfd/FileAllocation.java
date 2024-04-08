/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.kexec.HardwareTrackId;

public class FileAllocation {

    private final LogicalTrackExtent _logicalExtent;
    private final HardwareTrackId _hardwareTrackId;

    public FileAllocation(final LogicalTrackExtent region,
                          final HardwareTrackId hwTrackId) {
        _logicalExtent = new LogicalTrackExtent(region.getTrackId(), region.getTrackCount());
        _hardwareTrackId = new HardwareTrackId(hwTrackId.getLDATIndex(), hwTrackId.getTrackId());
    }

    public LogicalTrackExtent getFileRegion() { return _logicalExtent; }
    public HardwareTrackId getHardwareTrackId() { return _hardwareTrackId; }

    /**
     * Returns true if this file allocation is both logically and physically contiguous to (following)
     * the potential previous entry.
     * It is logically contiguous if the logical first track of this object is the next logical track
     * following the last logical track described by the potential previous entry.
     * It is physically contiguous if
     *      the LDAT index of the previous entry is the same as the LDAT index of this entry, and
     *      the physical track ID of this entry is equal to the sum of the physical track ID of the
     *          potential previous entry and the logical track count.
     * @param previous potential previous entry
     */
    public boolean isContiguousTo(final FileAllocation previous) {
        return _logicalExtent.isContiguousTo(previous.getFileRegion())
               && _hardwareTrackId.isContiguousTo(previous.getHardwareTrackId(), previous.getFileRegion().getTrackCount());
    }

    /**
     * Attempts to merge an extension allocation either to the front or the back of this allocation.
     * The attempt succeeds *only* if the extension is both logically and physically contiguous to
     * this entry.
     * @param extension proposed extension
     * @return true if the merge succeeds, else false
     */
    public boolean merge(final FileAllocation extension) {
        if (isContiguousTo(extension)) {
            // this object follows the extension
            _logicalExtent.setTrackId(extension.getFileRegion().getTrackId());
            _logicalExtent.addToTrackCount(extension.getFileRegion().getTrackCount());
            _hardwareTrackId.setTrackId(extension.getHardwareTrackId().getTrackId());
            return true;
        } else if (extension.isContiguousTo(this)) {
            // the extension follows this object
            _logicalExtent.addToTrackCount(extension.getFileRegion().getTrackCount());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof FileAllocation fa)
               && _logicalExtent.equals(fa._logicalExtent) && _hardwareTrackId.equals(fa._hardwareTrackId);
    }

    @Override
    public int hashCode() {
        return _logicalExtent.hashCode() ^ _hardwareTrackId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("[Rgn:%s HWTrk:%s", _logicalExtent.toString(), _hardwareTrackId.toString());
    }
}
