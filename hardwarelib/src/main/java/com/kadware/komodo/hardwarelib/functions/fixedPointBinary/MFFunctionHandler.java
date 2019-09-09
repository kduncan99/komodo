/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.fixedPointBinary;

import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import java.math.BigInteger;

/**
 * Handles the MF instruction f=032
 * This instruction is used to multiply fixed-point fractions when
 * the binary point is between bits 0 and 1; the product will then have an identically
 * positioned binary point.
 * The contents of U are fetched under j-field control and multiplied algebraically by the
 * contents of Aa. The resulting 72-bit product is then shifted left circularly by 1 bit
 * position. Subsequently, the shifted product is stored into Aa (36 most significant bits)
 * and Aa+1 (36 least significant bits).
 */
public class MFFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //TODO
        BigInteger operand1 = BigInteger.valueOf(ip.getExecOrUserARegister((int)iw.getA()).getW());
        BigInteger operand2 = BigInteger.valueOf(ip.getOperand(true,
                                                               true,
                                                               true,
                                                               true));
        DoubleWord36.StaticMultiplicationResult smr = DoubleWord36.multiply(operand1, operand2);
        DoubleWord36 result = new DoubleWord36(DoubleWord36.leftShiftCircular(smr._value, 1));
        Word36[] components = result.getWords();

        ip.getExecOrUserARegister((int) iw.getA()).setW(components[0].getW());
        ip.getExecOrUserARegister((int) iw.getA() + 1).setW(components[1].getW());
    }

    @Override
    public Instruction getInstruction() { return Instruction.MF; }
}
