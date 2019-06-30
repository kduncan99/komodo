/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.test;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.TestAndSetInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the TSS instruction f=073 j=017 a=01
 */
public class TSSFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        try {
            ip.testAndStore(true);
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        } catch (TestAndSetInterrupt ex) {
            //  lock already set - do nothing
        } finally {
            //  In any case, increment F0.x if/as appropriate
            ip.incrementIndexRegisterInF0();
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.TSS; }
}
