/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.InstructionWord;

import java.util.stream.IntStream;

public class ActivityStatePacket {

    private final ActiveBaseTable _activeBaseTable = new ActiveBaseTable();             // not in architectural ASP
    private final BaseRegister _baseRegisters[] = new BaseRegister[32];                 // not in architectural ASP
    private final InstructionWord _currentInstruction = new InstructionWord();
    private final DesignatorRegister _designatorRegister = new DesignatorRegister();
    private final GeneralRegisterSet _generalRegisterSet = new GeneralRegisterSet();    // not in architectural ASP
    private final IndicatorKeyRegister _indicatorKeyRegister = new IndicatorKeyRegister();
    private final ProgramAddressRegister _programAddressRegister = new ProgramAddressRegister();
    private final long _quantumTimer = 0;

    public ActivityStatePacket() {
        IntStream.range(0, 32).forEach(bx -> _baseRegisters[bx] = BaseRegister.createVoid());
    }

    public ActiveBaseTable getActiveBaseTable() { return _activeBaseTable; }

    public BaseRegister getBaseRegister(final int index) {
        if ((index < 0) || (index >= _baseRegisters.length)) {
            throw new RuntimeException(String.format("Invalid index=%d", index));
        }
        return _baseRegisters[index];
    }

    public InstructionWord getCurrentInstruction() { return _currentInstruction; }
    public DesignatorRegister getDesignatorRegister() { return _designatorRegister; }
    public IndicatorKeyRegister getIndicatorKeyRegister() { return _indicatorKeyRegister; }
    public GeneralRegisterSet getGeneralRegisterSet() { return _generalRegisterSet; }
    public ProgramAddressRegister getProgramAddressRegister() { return _programAddressRegister; }
    public long getQuantumTimer() { return _quantumTimer; }
}
