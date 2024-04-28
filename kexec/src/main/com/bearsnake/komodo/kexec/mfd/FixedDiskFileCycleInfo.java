/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import java.util.LinkedList;

public class FixedDiskFileCycleInfo extends DiskFileCycleInfo {

    private UnitSelectionIndicators _unitSelectionIndicators = new UnitSelectionIndicators();

    public FixedDiskFileCycleInfo(FileSetInfo fileSetInfo) {
        super(fileSetInfo);
    }

    public final UnitSelectionIndicators getUnitSelectionIndicators() {
        return _unitSelectionIndicators;
    }

    public final FixedDiskFileCycleInfo setUnitSelectionIndicators(final UnitSelectionIndicators value) {
        _unitSelectionIndicators = value;
        return this;
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
        var sector1 = mainItemSectors.get(1);

        sector0.setH1(27, _unitSelectionIndicators.compose());

        String paddedQualifier = String.format("%-12s", _qualifier);
        sector1.set(1, Word36.stringToWordFieldata(paddedQualifier.substring(0, 6)));
        sector1.set(2, Word36.stringToWordFieldata(paddedQualifier.substring(6)));

        String paddedFilename = String.format("%-12s", _filename);
        sector1.set(3, Word36.stringToWordFieldata(paddedFilename.substring(0, 6)));
        sector1.set(4, Word36.stringToWordFieldata(paddedFilename.substring(6)));
        sector1.set(5, Word36.stringToWordFieldata("*NO.1*"));
    }
}
