/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.generalStore;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the SNA instruction f=002
 */
public class SNAFunctionHandler extends FunctionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long op = ip.getExecOrUserARegister((int)iw.getA()).getW();
        ip.storeOperand(true, true, true, true, OnesComplement.negate36(op));
    }
}
