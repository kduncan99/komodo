/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.dayclockControl;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.Dayclock;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.misc.DesignatorRegister;

/**
 * Handles the RMD instruction f=075 j=017
 * Retrieves the system time as microseconds since epoch, shifted left by 5 bits and offset be a uniqueness value.
 * microseconds-since-epoch is adjusted by the system-wide dayclock offset before being shifted.
 */
@SuppressWarnings("Duplicates")
public class RMDFunctionHandler extends InstructionHandler {

    private static long _lastReportedMicros = 0;
    private static int _uniqueness = 0;

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DesignatorRegister dr = ip.getDesignatorRegister();
        if (dr.getProcessorPrivilege() > 2) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        ip.getJumpOperand(false);

        long result;
        long currentMicros = Dayclock.getDayclockMicros();
        synchronized (RMDFunctionHandler.class) {
            if (currentMicros != _lastReportedMicros) {
                _lastReportedMicros = currentMicros;
                _uniqueness = 0;
                result = _lastReportedMicros << 5;
            } else {
                ++_uniqueness;
                result = (_lastReportedMicros << 5) | _uniqueness;
            }
        }

        int regx = (int) iw.getA();
        ip.getExecOrUserARegister(regx).setW(result >> 36);
        ip.getExecOrUserARegister(regx + 1).setW(result);
    }

    @Override
    public Instruction getInstruction() { return Instruction.RMD; }
}
