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

    @Override
    public String toString() {
        return String.format("[Rgn:%s HWTrk:%s", _fileRegion.toString(), _hardwareTrackId.toString());
    }

    public TrackRegion getFileRegion() { return _fileRegion; }
    public HardwareTrackId getHardwareTrackId() { return _hardwareTrackId; }
}
