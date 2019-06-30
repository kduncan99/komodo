/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.shift;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the DSC instruction f=073 j=001
 */
public class DSCFunctionHandler extends InstructionHandler {

    private final long[] _operand = new long[2];
    private final long[] _result = new long[2];

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        _operand[0] = ip.getExecOrUserARegister((int)iw.getA()).getW();
        _operand[1] = ip.getExecOrUserARegister((int)iw.getA() + 1).getW();
        int count = (int)ip.getImmediateOperand() & 0177;
        OnesComplement.rightShiftCircular72(_operand, count, _result);

        ip.getExecOrUserARegister((int)iw.getA()).setW(_result[0]);
        ip.getExecOrUserARegister((int)iw.getA() + 1).setW(_result[1]);
    }

    @Override
    public Instruction getInstruction() { return Instruction.DSC; }
}
