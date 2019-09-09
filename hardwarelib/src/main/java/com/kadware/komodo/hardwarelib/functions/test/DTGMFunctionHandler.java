/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.test;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the DTGM instruction extended mode f=033 j=014
 */
public class DTGMFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Skip NI if |(U,U+1)| > A(a),A(a+1)

        long[] uValue = new long[2];
        ip.getConsecutiveOperands(true, uValue);
        DoubleWord36 dwu = new DoubleWord36(uValue[0], uValue[1]);
        if (dwu.isNegative()) {
            dwu = dwu.negate();
        }

        DoubleWord36 dwa = new DoubleWord36(ip.getExecOrUserARegister((int)iw.getA()).getW(),
                                            ip.getExecOrUserARegister((int)iw.getA() + 1).getW());
        if (dwu.compareTo(dwa) > 0) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.DTGM; }
}
