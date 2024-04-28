/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

/**
 * Describes one disk pack upon which a removable disk file may be stored partially or fully
 */
public final class DiskPackEntry {

    private final String _packName;
    private final MFDRelativeAddress _mainItem0Addr;

    public DiskPackEntry(
        final String packName,
        final MFDRelativeAddress mainItem0Address
    ) {
        _packName = packName;
        _mainItem0Addr = mainItem0Address;
    }

    public String getPackName() { return _packName; }
    public MFDRelativeAddress getMainItem0Address() { return _mainItem0Addr; }
}
