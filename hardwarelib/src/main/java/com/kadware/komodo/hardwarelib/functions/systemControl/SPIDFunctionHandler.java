/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.systemControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the SPID instruction f=073 j=015 a=005
 */
public class SPIDFunctionHandler extends InstructionHandler {

    //  Mitigates object proliferation, but requires sync'd thread protection
    private final long operands[] = new long[2];

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  PP has to be at last 2
        int procPriv = ip.getDesignatorRegister().getProcessorPrivilege();
        if (procPriv > 2) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        //  Operand 0:
        //      MSBit is 1
        //      T1/T2 contains feature bits (which we don't do)
        //      T3 contains UPI number (but only for pp < 2)
        operands[0] = (1l << 35) | ((procPriv < 2) ? ip._upiIndex : 0l);

        //  Operand 1:
        //      MSBit is 0
        //      Q1 is Series (for M series, this is zero.  That's us.)
        //      Q2 is Model - we use 3 (latest recognized - for Dorado)
        //      H2 is reserved
        operands[1] = (3l << 18);
        ip.storeConsecutiveOperands(true, operands);
    }

    @Override
    public Instruction getInstruction() { return Instruction.SPID; }
}
