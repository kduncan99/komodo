/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.fixedPointBinary;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.baselib.Word36;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the AH instruction f=072 j=04
 */
public class AHFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand1 = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long operand2 = ip.getOperand(true, true, false, false);

        long op1h1 = Word36.getH1(operand1);
        long op1h2 = Word36.getH2(operand1);
        long op2h1 = Word36.getH1(operand2);
        long op2h2 = Word36.getH2(operand2);

        long resulth1 = OnesComplement.add18Simple(op1h1, op2h1);
        long resulth2 = OnesComplement.add18Simple(op1h2, op2h2);
        long result = (resulth1 << 18) | resulth2;

        ip.getExecOrUserARegister((int)iw.getA()).setW(result);
    }

    @Override
    public Instruction getInstruction() { return Instruction.AH; }
}
