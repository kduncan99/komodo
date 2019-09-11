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
 * Handles the DI instruction f=034
 * 72-bit signed dividend in A(a)|A(a+1) is divided by the 36-bit divisor in U.
 * The 36-bit quotient stored in A(a), and the 36-bit remainder in A(a+1).
 * Divide check raised if |dividend| >= ( |divisor| * 2^35 ) or divisor is +/- 0
 */
@SuppressWarnings("Duplicates")
public class DIFunctionHandler extends InstructionHandler {

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

        long[] divisor = new long[2];
        divisor[1] = ip.getOperand(true, true, true, true);
        divisor[0] = Word36.isNegative(divisor[1]) ? Word36.NEGATIVE_ZERO : Word36.POSITIVE_ZERO;

        long quotient = 0;
        long remainder = 0;

        DoubleWord36 dwDividend = new DoubleWord36(dividend[0], dividend[1]);
        DoubleWord36 dwDivisor = new DoubleWord36(divisor[0], divisor[1]);
        BigInteger compDividend = dwDividend.get().abs();
        BigInteger compDivisor = dwDivisor.get().abs().shiftLeft(35);
        if (dwDivisor.isZero() || (compDividend.compareTo(compDivisor) >= 0)) {
            ip.getDesignatorRegister().setDivideCheck(true);
            if ( ip.getDesignatorRegister()
                .getArithmeticExceptionEnabled() ) {
                throw new ArithmeticExceptionInterrupt(ArithmeticExceptionInterrupt.Reason.DivideCheck);
            }
        } else {
            DoubleWord36.DivisionResult dr = dwDividend.divide(dwDivisor);
            quotient = dr._result.get().longValue() & Word36.BIT_MASK;
            remainder = dr._remainder.get().longValue() & Word36.BIT_MASK;
        }

        ip.setExecOrUserARegister((int) iw.getA(), quotient);
        ip.setExecOrUserARegister((int) iw.getA() + 1, remainder);
    }

    @Override
    public Instruction getInstruction() { return Instruction.DI; }
}
