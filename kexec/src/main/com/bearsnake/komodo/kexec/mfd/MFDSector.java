/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.ArraySlice;

/**
 * A tuple which contains an MFDSector ArraySlice reference, along with its corresponding MFDRelativeAddress
 */
public class MFDSector {

    private final MFDRelativeAddress _address;
    private final ArraySlice _sector;

    public MFDSector(
        final MFDRelativeAddress address,
        final ArraySlice sector
    ) {
        _address = address;
        _sector = sector;
    }

    public MFDRelativeAddress getAddress() { return _address; }
    public ArraySlice getSector() { return _sector; }
}
