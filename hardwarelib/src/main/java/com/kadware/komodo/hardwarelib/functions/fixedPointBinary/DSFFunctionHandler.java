/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.fixedPointBinary;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.ArithmeticExceptionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the DSF instruction f=035
 */
public class DSFFunctionHandler extends InstructionHandler {

    /*TODO
        [(Aa || 36 sign bits) RIGHT ALGEBRAIC SHIFT 1]  (U)  Aa+1
        The 36-bit signed contents of Aa concatenated with 36 sign bits on the right (the dividend) are
        fetched, shifted right algebraically 1 bit position, and divided algebraically by the contents of U (the
        divisor), which is fetched under F0.j control. The 36-bit signed quotient is then stored into Aa+1
        and the remainder is discarded.
        A divide check occurs if the absolute value of the dividend (Aa) is not less than the absolute value
        of the divisor (U), or if the divisor = 0. Divide check is handled as described in 6.3.
     */

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  A(a)//36 sign bits gets algebraically shifted right one bit, then divided by 36-bit U.
        //  The 36-bit result is stored in A(a+1), the remainder is lost.
        //  I have no idea how one would use this.
        _dividend[0] = ip.getExecOrUserARegister((int)iw.getA()).getW();
        _dividend[1] = OnesComplement.isNegative36(_dividend[0]) ? OnesComplement.NEGATIVE_ZERO_36 : OnesComplement.POSITIVE_ZERO_36;
        OnesComplement.rightShiftAlgebraic72(_dividend, 1, _dividend);

        _divisor[1] = ip.getOperand(true, true, true, true);
        _divisor[0] = OnesComplement.isNegative36(_divisor[1]) ? OnesComplement.NEGATIVE_ZERO_36 : OnesComplement.POSITIVE_ZERO_36;

        long quotient = 0;
        try {
            OnesComplement.divide72(_dividend, _divisor, _dr);
            if (!OnesComplement.isZero36(_dr._quotient[0])) {
                //  quotient is too large - divide check, and maybe an interrupt
                ip.getDesignatorRegister().setDivideCheck(true);
                if (ip.getDesignatorRegister().getArithmeticExceptionEnabled()) {
                    throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.DivideCheck);
                }
            } else {
                quotient = _dr._quotient[1];
            }
        } catch (DivideByZeroException ex) {
            //  divisor is zero - divide check, and maybe an interrupt
            ip.getDesignatorRegister().setDivideCheck(true);
            if (ip.getDesignatorRegister().getArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.DivideCheck);
            }
        }

        ip.getExecOrUserARegister((int)iw.getA() + 1).setW(quotient);
    }

    @Override
    public Instruction getInstruction() { return Instruction.DSF; }
}
