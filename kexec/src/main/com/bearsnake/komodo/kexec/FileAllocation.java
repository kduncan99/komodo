/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

public class FileAllocation {

    private final TrackRegion _fileRegion;
    private final HardwareTrackId _hardwareTrackId;

    public FileAllocation(final TrackRegion region,
                          final HardwareTrackId hwTrackId) {
        _fileRegion = new TrackRegion(region.getTrackId(), region.getTrackCount());
        _hardwareTrackId = new HardwareTrackId(hwTrackId.getLDATIndex(), hwTrackId.getTrackId());
    }

    public TrackRegion getFileRegion() { return _fileRegion; }
    public HardwareTrackId getHardwareTrackId() { return _hardwareTrackId; }

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof FileAllocation fa)
            && _fileRegion.equals(fa._fileRegion) && _hardwareTrackId.equals(fa._hardwareTrackId);
    }

    @Override
    public int hashCode() {
        return _fileRegion.hashCode() ^ _hardwareTrackId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("[Rgn:%s HWTrk:%s", _fileRegion.toString(), _hardwareTrackId.toString());
    }
}
