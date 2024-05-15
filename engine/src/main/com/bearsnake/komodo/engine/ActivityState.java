/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import java.util.stream.IntStream;

public class ActivityState {

    private final BaseRegister _baseRegisters[] = new BaseRegister[32];
    private final DesignatorRegister _designatorRegister = new DesignatorRegister();
    private final IndicatorKeyRegister _indicatorKeyRegister = new IndicatorKeyRegister();
    private final GeneralRegisterSet _generalRegisterSet = new GeneralRegisterSet();
    private final ProgramAddressRegister _programAddressRegister = new ProgramAddressRegister();

    public BaseRegister getBaseRegister(final int index) { return _baseRegisters[index]; }
    public DesignatorRegister getDesignatorRegister() { return _designatorRegister; }
    public GeneralRegisterSet getGeneralRegisterSet() { return _generalRegisterSet; }
    public IndicatorKeyRegister getIndicatorKeyRegister() { return _indicatorKeyRegister; }
    public ProgramAddressRegister getProgramAddressRegister() { return _programAddressRegister; }

    public ActivityState() {
        IntStream.range(0, 32).forEach(bx -> _baseRegisters[bx] = new BaseRegister());
    }
}
