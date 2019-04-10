/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.fixedPointBinary;

import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.ArithmeticExceptionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the DSF instruction f=035
 */
public class DSFFunctionHandler extends FunctionHandler {

    private final long[] _dividend = { 0, 0 };
    private final long[] _divisor = { 0, 0 };
    private final OnesComplement.DivideResult _dr = new OnesComplement.DivideResult();

    @Override
    public synchronized void handle(
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
}
