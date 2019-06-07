/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.fixedPointBinary;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the MF instruction f=032
 * <p>
 * This instruction is used to multiply fixed-point fractions when
 * the binary point is between bits 0 and 1; the product will then have an identically
 * positioned binary point.
 * <p>
 * The contents of U are fetched under j-field control and multiplied algebraically by the
 * contents of Aa. The resulting 72-bit product is then shifted left circularly by 1 bit
 * position. Subsequently, the shifted product is stored into Aa (36 most significant bits)
 * and Aa+1 (36 least significant bits).
 */
public class MFFunctionHandler extends InstructionHandler {

    private final long[] _product = { 0, 0 };

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand1 = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long operand2 = ip.getOperand(true, true, true, true);
        OnesComplement.multiply36(operand1, operand2, _product);
        OnesComplement.leftShiftCircular72(_product, 1, _product);

        ip.getExecOrUserARegister((int)iw.getA()).setW(_product[0]);
        ip.getExecOrUserARegister((int)iw.getA() + 1).setW(_product[1]);
    }

    @Override
    public Instruction getInstruction() { return Instruction.MF; }
}
