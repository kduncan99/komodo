/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.fixedPointBinary;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.baselib.exceptions.DivideByZeroException;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.ArithmeticExceptionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the DF instruction f=036
 * 72-bit signed dividend in A(a)|A(a+1) is shifted right algebraically by one bit,
 * then divided by the 36-bit divisor in U, with the 36-bit quotient stored in A(a)
 * and the 36-bit remainder in A(a+1).
 */
@SuppressWarnings("Duplicates")
public class DFFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long[] dividend = {
            ip.getExecOrUserARegister((int) iw.getA()).getW(),
            ip.getExecOrUserARegister((int) iw.getA() + 1).getW()
        };
        DoubleWord36 dwDividend = new DoubleWord36(dividend[0], dividend[1]).rightShiftAlgebraic(1);

        long quotient = 0;
        long remainder = 0;

        try {
            long[] divisor = new long[2];
            divisor[1] = ip.getOperand(true, true, true, true);
            if (Word36.isZero(divisor[1])) {
                //  divide by zero
                throw new DivideByZeroException();
            }

            divisor[0] = Word36.isNegative(divisor[1]) ? Word36.NEGATIVE_ZERO : Word36.POSITIVE_ZERO;
            DoubleWord36 dwDivisor = new DoubleWord36(divisor[0], divisor[1]);

            DoubleWord36.DivisionResult dr = dwDividend.divide(dwDivisor);

            quotient = dr._result.get().shiftRight(36).longValue() & Word36.BIT_MASK;
            if ((quotient & Word36.BIT_MASK) != quotient) {
                //  quotient is too big for the result register
                /*TODO we did this wrong... see below
                    A divide check occurs if the absolute value of the dividend (Aa,Aa+1) is not less than the absolute
                    value of the divisor (U) multiplied by 235, or if the divisor = 0. Divide check is handled as
                    described in 6.3.
                */
                throw new DivideByZeroException();
            }
            remainder = dr._result.get().longValue() & Word36.BIT_MASK;
        } catch (DivideByZeroException ex) {
            //  divisor is zero - divide check, and maybe an interrupt.
            //  If no interrupt, just drop through, resulting in setting the result registers to zero (per arch. document)
            ip.getDesignatorRegister().setDivideCheck(true);
            if (ip.getDesignatorRegister().getArithmeticExceptionEnabled()) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.DivideCheck);
            }
        }

        ip.getExecOrUserARegister((int) iw.getA()).setW(quotient);
        ip.getExecOrUserARegister((int) iw.getA() + 1).setW(remainder);
    }

    @Override
    public Instruction getInstruction() { return Instruction.DF; }
}
