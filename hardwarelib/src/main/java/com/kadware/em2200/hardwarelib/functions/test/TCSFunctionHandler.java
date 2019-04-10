/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.test;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.TestAndSetInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the TCS instruction f=073 j=017 a=02
 */
public class TCSFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        try {
            ip.testAndStore(false);
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        } catch (TestAndSetInterrupt ex) {
            //  lock already clear - do nothing
        } finally {
            //  In any case, increment F0.x if/as appropriate
            ip.incrementIndexRegisterInF0();
        }
    }
}
