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
 * Handles the ANT instruction f=072 j=06
 */
@SuppressWarnings("Duplicates")
public class ANTFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand1 = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long operand2 = ip.getOperand(true, true, false, false);

        long op1t1 = Word36.getSignExtended12(Word36.getT1(operand1));
        long op1t2 = Word36.getSignExtended12(Word36.getT2(operand1));
        long op1t3 = Word36.getSignExtended12(Word36.getT3(operand1));
        long op2t1 = Word36.negate(Word36.getSignExtended12(Word36.getT1(operand2)));
        long op2t2 = Word36.negate(Word36.getSignExtended12(Word36.getT2(operand2)));
        long op2t3 = Word36.negate(Word36.getSignExtended12(Word36.getT3(operand2)));

        long resultt1 = Word36.addSimple(op1t1, op2t1) & 07777;
        long resultt2 = Word36.addSimple(op1t2, op2t2) & 07777;
        long resultt3 = Word36.addSimple(op1t3, op2t3) & 07777;
        long result = (resultt1 << 24) | (resultt2 << 12) | resultt3;

        ip.setExecOrUserARegister((int) iw.getA(), result);
    }

    @Override
    public Instruction getInstruction() { return Instruction.ANT; }
}
