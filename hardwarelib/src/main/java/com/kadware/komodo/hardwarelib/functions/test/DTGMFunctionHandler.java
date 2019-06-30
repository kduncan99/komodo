/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.test;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import java.math.BigInteger;

/**
 * Handles the DTGM instruction extended mode f=033 j=014
 */
public class DTGMFunctionHandler extends InstructionHandler {

    private final long[] _aValue = new long[2];
    private final long[] _uValue = new long[2];

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Skip NI if |(U,U+1)| > A(a),A(a+1)

        ip.getConsecutiveOperands(true, _uValue);
        if (OnesComplement.isNegative72(_uValue)) {
            OnesComplement.negate72(_uValue, _uValue);
        }

        _aValue[0] = ip.getExecOrUserARegister((int)iw.getA()).getW();
        _aValue[1] = ip.getExecOrUserARegister((int)iw.getA() + 1).getW();

        BigInteger biUValue = OnesComplement.getNative72(_uValue);
        BigInteger biAValue = OnesComplement.getNative72(_aValue);
        if (biUValue.compareTo(biAValue) > 0) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.DTGM; }
}
