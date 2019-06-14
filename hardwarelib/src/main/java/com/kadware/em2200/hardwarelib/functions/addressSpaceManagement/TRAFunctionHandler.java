/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.addressSpaceManagement;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.ReferenceViolationInterrupt;
import com.kadware.em2200.hardwarelib.misc.BaseRegister;

/**
 * Handles the TRA instruction f=072 j=015
 */
public class TRAFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int brIndex = ip.getBasicModeBankRegisterIndex();
        long result = ((brIndex == 0) ? 0 : 0400000_000000L) | ((long) (brIndex & 03) << 33);
        ip.getExecOrUserXRegister((int) iw.getA()).setW(result);

        if (brIndex != 0) {
            try {
                BaseRegister bReg = ip.getBaseRegister(brIndex);
                bReg.checkAccessLimits(false,
                                       true,
                                       true,
                                       ip.getIndicatorKeyRegister().getAccessInfo());
                ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
            } catch (ReferenceViolationInterrupt ex) {
                //  do nothing
            }
        }
    }

    @Override
    public Instruction getInstruction() {
        return Instruction.TRA;
    }
}

