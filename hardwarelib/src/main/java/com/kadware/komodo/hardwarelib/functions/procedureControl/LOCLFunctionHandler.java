/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.procedureControl;

import com.kadware.komodo.baselib.IndexRegister;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the LOCL instruction f=07 j=016 a=00
 */
public class LOCLFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        rcsPush(ip, 0);
        IndexRegister xReg = ip.getExecOrUserXRegister(0);
        long newXValue = IndexRegister.setH1(xReg.getW(), ip.getDesignatorRegister().getBasicModeEnabled() ? 0_400000_000000L : 0);
        newXValue = IndexRegister.setH2(newXValue, ip.getIndicatorKeyRegister().getAccessInfo().get());
        ip.setExecOrUserXRegister(0, newXValue);
        int counter = ip.getJumpOperand(true);
        ip.setProgramCounter(counter, true);
    }

    @Override
    public Instruction getInstruction() { return Instruction.LOCL; }
}
