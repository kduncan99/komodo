/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.fixedPointBinary;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the MI instruction f=030
 */
public class MIFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DoubleWord36 factor1 = new DoubleWord36(0, ip.getExecOrUserARegister((int) iw.getA()).getW());
        DoubleWord36 factor2 = new DoubleWord36(0, ip.getOperand(true,
                                                                 true,
                                                                 true,
                                                                 true));
        DoubleWord36.MultiplicationResult mr = factor1.multiply(factor2);
        Word36[] resultWords = mr._value.getWords();
        ip.getExecOrUserARegister((int) iw.getA()).setW(resultWords[0].getW());
        ip.getExecOrUserARegister((int) iw.getA() + 1).setW(resultWords[1].getW());
    }

    @Override
    public Instruction getInstruction() { return Instruction.MI; }
}
