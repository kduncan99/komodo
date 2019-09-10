/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.generalLoad;

import com.kadware.komodo.baselib.IndexRegister;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the LXLM instruction f=075 j=013 EM any PP, BM PP = 0
 */
public class LXLMFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        if ((ip.getDesignatorRegister().getProcessorPrivilege() > 0) && (ip.getDesignatorRegister().getBasicModeEnabled())) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        long operand = ip.getOperand(true, true, false, false);
        int ixReg = (int) iw.getA();
        IndexRegister xReg = ip.getExecOrUserXRegister(ixReg);
        ip.setExecOrUserXRegister(ixReg, IndexRegister.setXM24(xReg.getW(), operand));
    }

    @Override
    public Instruction getInstruction() { return Instruction.LXLM; }
}
