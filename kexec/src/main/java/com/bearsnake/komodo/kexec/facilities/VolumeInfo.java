/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import java.io.PrintStream;

/**
 * Describes a tape volume
 */
public class VolumeInfo implements MediaInfo {

    private String            _volumeName;

    public VolumeInfo() {}

    public String getVolumeName() { return _volumeName; }
    public VolumeInfo setVolumeName(final String value) { _volumeName = value; return this; }

    @Override
    public String getMediaName() { return _volumeName; }

    @Override
    public void dump(final PrintStream out, final String indent) {
        // TODO
    }
}
