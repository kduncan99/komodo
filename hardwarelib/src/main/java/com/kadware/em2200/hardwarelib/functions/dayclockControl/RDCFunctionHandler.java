/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.dayclockControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.Dayclock;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.misc.DesignatorRegister;

/**
 * Handles the RDC instruction f=075 j=012
 */
@SuppressWarnings("Duplicates")
public class RDCFunctionHandler extends InstructionHandler {

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

        long micros = Dayclock.getDayclockMicros();
        int regx = (int) iw.getA();
        ip.getExecOrUserARegister(regx).setW(micros >> 36);
        ip.getExecOrUserARegister(regx + 1).setW(micros);
    }

    @Override
    public Instruction getInstruction() { return Instruction.RDC; }
}
