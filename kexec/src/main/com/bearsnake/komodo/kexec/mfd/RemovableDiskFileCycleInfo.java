/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import java.util.LinkedList;

public class RemovableDiskFileCycleInfo extends DiskFileCycleInfo {

    public RemovableDiskFileCycleInfo(FileSetInfo fileSetInfo) {
        super(fileSetInfo);
    }

    /**
     * Populates cataloged file main item sectors 0 and 1
     * Invokes super class to do the most common things, then fills in anything related to mass storage
     */
    @Override
    public void populateMainItems(
        final LinkedList<ArraySlice> mainItemSectors
    ) throws ExecStoppedException {
        super.populateMainItems(mainItemSectors);
        var sector0 = mainItemSectors.get(0);

        var wReadKey = Word36.stringToWordFieldata(_fileSetInfo.getReadKey());
        var wWriteKey = Word36.stringToWordFieldata(_fileSetInfo.getWriteKey());
        sector0.setH1(24, wReadKey >> 18);
        sector0.setH1(25, wReadKey & 0_777777);
        sector0.setH1(26, wWriteKey >> 18);
        sector0.setH1(27, wWriteKey & 0_777777);
    }
}
