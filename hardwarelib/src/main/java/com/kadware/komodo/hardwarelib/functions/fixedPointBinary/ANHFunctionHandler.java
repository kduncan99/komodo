/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.fixedPointBinary;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the ANH instruction f=072 j=05
 */
public class ANHFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand1 = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long operand2 = ip.getOperand(true, true, false, false);

        long op1h1 = Word36.getSignExtended18(Word36.getH1(operand1));
        long op1h2 = Word36.getSignExtended18(Word36.getH2(operand1));
        long op2h1 = Word36.negate(Word36.getSignExtended18(Word36.getH1(operand2)));
        long op2h2 = Word36.negate(Word36.getSignExtended18(Word36.getH2(operand2)));

        long resulth1 = Word36.addSimple(op1h1, op2h1);
        long resulth2 = Word36.addSimple(op1h2, op2h2);
        long result = ((resulth1 << 18) & 0_777777) | (resulth2 & 0_777777);

        ip.getExecOrUserARegister((int)iw.getA()).setW(result);
    }

    @Override
    public Instruction getInstruction() { return Instruction.ANH; }
}
