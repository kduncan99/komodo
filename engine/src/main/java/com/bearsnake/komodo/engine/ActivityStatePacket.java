/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.InstructionWord;

public class ActivityStatePacket {

    private final InstructionWord _currentInstruction = new InstructionWord();
    private final DesignatorRegister _designatorRegister = new DesignatorRegister();
    private final IndicatorKeyRegister _indicatorKeyRegister = new IndicatorKeyRegister();
    private final ProgramAddressRegister _programAddressRegister = new ProgramAddressRegister();
    private final long _quantumTimer = 0;

    public ActivityStatePacket() {
    }

    public InstructionWord getCurrentInstruction() { return _currentInstruction; }
    public DesignatorRegister getDesignatorRegister() { return _designatorRegister; }
    public IndicatorKeyRegister getIndicatorKeyRegister() { return _indicatorKeyRegister; }
    public ProgramAddressRegister getProgramAddressRegister() { return _programAddressRegister; }
    public long getQuantumTimer() { return _quantumTimer; }

    public ActivityStatePacket setCurrentInstruction(
        final long instruction
    ) {
        _currentInstruction.setW(instruction);
        return this;
    }
}
