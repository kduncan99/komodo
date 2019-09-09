/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.fixedPointBinary;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.ArithmeticExceptionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

import java.math.BigInteger;

/**
 * Handles the DSF instruction f=035
 * A(a)||36 sign bits gets algebraically shifted right one bit, then divided by 36-bit U.
 * The 36-bit result is stored in A(a+1), the remainder is lost.
 * I have no idea how one would use this.
 * A divide check is raised if |dividend| not < |divisor| or if divisor = +/- zero
 */
@SuppressWarnings("Duplicates")
public class DSFFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long[] dividend = new long[2];
        dividend[0] = ip.getExecOrUserARegister((int) iw.getA()).getW();
        dividend[1] = Word36.isNegative(dividend[0]) ? Word36.BIT_MASK : 0;

        long[] divisor = new long[2];
        divisor[1] = ip.getOperand(true, true, true, true);
        divisor[0] = Word36.isNegative(divisor[1]) ? Word36.NEGATIVE_ZERO : Word36.POSITIVE_ZERO;

        long quotient = 0;
        DoubleWord36 dwDividend = new DoubleWord36(dividend[0], dividend[1]).rightShiftAlgebraic(1);
        DoubleWord36 dwDivisor = new DoubleWord36(divisor[0], divisor[1]);
        if (dwDivisor.isZero() || (dividend[0] >= divisor[1])) {
            ip.getDesignatorRegister().setDivideCheck(true);
            if ( ip.getDesignatorRegister()
                .getArithmeticExceptionEnabled() ) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.DivideCheck);
            }
        } else {
            DoubleWord36.DivisionResult dr = dwDividend.divide(dwDivisor);
            quotient = dr._result.get().shiftRight(36).longValue() & Word36.BIT_MASK;
        }

        ip.getExecOrUserARegister((int) iw.getA() + 1).setW(quotient);
    }

    @Override
    public Instruction getInstruction() { return Instruction.DSF; }
}
