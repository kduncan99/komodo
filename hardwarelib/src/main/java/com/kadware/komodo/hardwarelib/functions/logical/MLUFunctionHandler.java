/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.logical;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the MLU instruction f=043
 */
public class MLUFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long opAa = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long opR2 = ip.getExecOrUserRRegister(2).getW();
        long compR2 = Word36.negate(opR2);
        long opU = ip.getOperand(true, true, true, true);
        long result = (opU & opR2) | (opAa & compR2);
        ip.getExecOrUserARegister((int)iw.getA() + 1).setW(result);
    }

    @Override
    public Instruction getInstruction() { return Instruction.MLU; }
}
