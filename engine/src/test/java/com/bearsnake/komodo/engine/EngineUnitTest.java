/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.ArraySlice;

public abstract class EngineUnitTest {

    protected Engine _engine;

    /**
     * For base register 0 and 16-31
     */
    protected void loadBaseRegister(
        final int registerNumber,
        final boolean isLargeBank,
        final int lowerLimitNormalized,
        final int upperLimitNormalized,
        final AbsoluteAddress baseAddress,
        final ArraySlice storage
    ) {
        _engine.getBaseRegister(registerNumber)
               .setIsLargeBank(isLargeBank)
               .setLimitsNormalized(false, lowerLimitNormalized, upperLimitNormalized)
               .setBaseAddress(baseAddress)
               .setStorage(storage);
    }

    /**
     * For base registers 1 through 15
     */
    protected void loadBaseRegister(
        final int registerNumber,
        final boolean isLargeBank,
        final int lowerLimitNormalized,
        final int upperLimitNormalized,
        final AbsoluteAddress baseAddress,
        final ArraySlice storage,
        final int bankLevel,
        final int bankIndex,
        final int subsetting
    ) {
        loadBaseRegister(registerNumber, isLargeBank, lowerLimitNormalized, upperLimitNormalized, baseAddress, storage);
        var abte = _engine.getActiveBaseTableEntry(registerNumber);
        abte.setBankLevel((short)bankLevel);
        abte.setBankDescriptorIndex((short)bankIndex);
        abte.setSubsetSpecification(subsetting);
    }
}
