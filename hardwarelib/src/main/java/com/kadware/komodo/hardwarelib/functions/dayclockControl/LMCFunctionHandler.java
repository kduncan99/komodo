/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.dayclockControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.DesignatorRegister;

/**
 * Handles the LMC instruction f=037 j=007
 */
@SuppressWarnings("Duplicates")
public class LMCFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DesignatorRegister dr = ip.getDesignatorRegister();
        if (dr.getProcessorPrivilege() > 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        ip.getJumpOperand(false);
        int regx = (int) iw.getA();
        long micros = (ip.getExecOrUserARegister(regx).getH2() << 36) | ip.getExecOrUserARegister(regx + 1).getW();
        ip.setDayclockComparator(micros);
    }

    @Override
    public Instruction getInstruction() { return Instruction.LMC; }
}
