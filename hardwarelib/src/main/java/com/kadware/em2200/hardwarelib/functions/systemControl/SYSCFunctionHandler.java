/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.systemControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the SYSC instruction f=073 j=017 a=012
 */
public class SYSCFunctionHandler extends InstructionHandler {

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  PP has to be 0
        int procPriv = ip.getDesignatorRegister().getProcessorPrivilege();
        if (procPriv > 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        //  Retrieve U.  This could be troublesome in the future, as we need to retrieve maybe more than one value
        //  starting at U, but we don't know how many words until we know the subfunction, which is in U+0.
        //  But... due to storage lock logic, we're not allowed to ask multiple times.  So...  not a problem yet.
        long operand = ip.getOperand(false, false, false, false);

        //  For now, we do not recognize any sub-functions, so we always throw a machine interrupt
        throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
    }

    @Override
    public Instruction getInstruction() { return Instruction.SYSC; }
}
