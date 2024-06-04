/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import java.util.LinkedList;

public class FixedDiskFileCycleInfo extends DiskFileCycleInfo {

    private UnitSelectionIndicators _unitSelectionIndicators = new UnitSelectionIndicators();

    public final UnitSelectionIndicators getUnitSelectionIndicators() {
        return _unitSelectionIndicators;
    }

    public final FixedDiskFileCycleInfo setUnitSelectionIndicators(final UnitSelectionIndicators value) {
        _unitSelectionIndicators = value;
        return this;
    }

    /**
     * Loads this object from the content in the given main item MFD sector chain.
     * @param mfdSectors main item chain
     */
    public void loadFromMainItemChain(
        final LinkedList<MFDSector> mfdSectors
    ) {
        super.loadFromMainItemChain(mfdSectors);
        var sector0 = mfdSectors.getFirst().getSector();
        _unitSelectionIndicators = new UnitSelectionIndicators().extract(sector0.getH1(033));
    }

    /**
     * Populates cataloged file main item sectors 0 and 1.
     * Invokes super class to do the most common things, then fills in anything related to mass storage.
     * _leadItem0Address must be populated before invoking
     * @param mfdSectors enough MFDSectors to store all the information required for this file cycle.
     */
    @Override
    public int populateMainItems(
        final LinkedList<MFDSector> mfdSectors
    ) throws ExecStoppedException {
        int result = super.populateMainItems(mfdSectors);
        mfdSectors.getFirst().getSector().setH1(27, _unitSelectionIndicators.compose());
        return result;
    }
}
