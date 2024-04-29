/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import java.util.LinkedList;

public class RemovableDiskFileCycleInfo extends DiskFileCycleInfo {

    private String _readKey = "";
    private String _writeKey = "";

    public RemovableDiskFileCycleInfo(
        final MFDSector leadItem0
    ) {
        super(leadItem0);
    }

    public final String getReadKey() { return _readKey; }
    public final String getWriteKey() { return _writeKey; }

    public RemovableDiskFileCycleInfo setReadKey(final String readKey) { _readKey = readKey; return this; }
    public RemovableDiskFileCycleInfo setWriteKey(final String writeKey) { _writeKey = writeKey; return this; }

    /**
     * Populates cataloged file main item sectors 0 and 1
     * Invokes super class to do the most common things, then fills in anything related to mass storage
     * @param mfdSectors enough MFDSectors to store all of the information required for this file cycle.
     */
    @Override
    public void populateMainItems(
        LinkedList<MFDSector> mfdSectors
    ) throws ExecStoppedException {
        super.populateMainItems(mfdSectors);
        var sector0 = mfdSectors.get(0).getSector();

        var wReadKey = Word36.stringToWordFieldata(_readKey);
        var wWriteKey = Word36.stringToWordFieldata(_writeKey);
        sector0.setH1(24, wReadKey >> 18);
        sector0.setH1(25, wReadKey & 0_777777);
        sector0.setH1(26, wWriteKey >> 18);
        sector0.setH1(27, wWriteKey & 0_777777);
    }
}
