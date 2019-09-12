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
import java.math.BigInteger;

/**
 * Handles the MI instruction f=030
 */
@SuppressWarnings("Duplicates")
public class MIFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        BigInteger factor1 = BigInteger.valueOf(ip.getExecOrUserARegister((int) iw.getA()).getW());
        BigInteger sgnExFactor1 = DoubleWord36.extendSign(factor1, 36);
        BigInteger factor2 = BigInteger.valueOf(ip.getOperand(true, true, true, true));
        BigInteger sgnExFactor2 = DoubleWord36.extendSign(factor2, 36);
        DoubleWord36.StaticMultiplicationResult smr = DoubleWord36.multiply(sgnExFactor1, sgnExFactor2);
        Word36[] resultWords = DoubleWord36.getWords(smr._value);

        ip.setExecOrUserARegister((int) iw.getA(), resultWords[0].getW());
        ip.setExecOrUserARegister((int) iw.getA() + 1, resultWords[1].getW());
    }

    @Override
    public Instruction getInstruction() { return Instruction.MI; }
}
