/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import java.util.LinkedList;

public class FixedDiskFileCycleInfo extends DiskFileCycleInfo {

    private UnitSelectionIndicators _unitSelectionIndicators = new UnitSelectionIndicators();

    public FixedDiskFileCycleInfo(
        final MFDSector leadItem0
    ) {
        super(leadItem0);
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
     * @param mfdSectors enough MFDSectors to store all of the information required for this file cycle.
     */
    @Override
    public void populateMainItems(
        LinkedList<MFDSector> mfdSectors
    ) throws ExecStoppedException {
        super.populateMainItems(mfdSectors);
        mfdSectors.getFirst().getSector().setH1(27, _unitSelectionIndicators.compose());
    }
}
