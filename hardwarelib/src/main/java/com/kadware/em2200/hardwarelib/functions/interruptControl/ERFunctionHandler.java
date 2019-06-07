/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.interruptControl;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.*;

/**
 * Handles the ER instruction f=072 j=011 a=unused - basic mode only
 */
public class ERFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand = ip.getImmediateOperand();
        throw new SignalInterrupt(SignalInterrupt.SignalType.ExecutiveRequest, (int) operand);
    }

    @Override
    public Instruction getInstruction() { return Instruction.ER; }
}
