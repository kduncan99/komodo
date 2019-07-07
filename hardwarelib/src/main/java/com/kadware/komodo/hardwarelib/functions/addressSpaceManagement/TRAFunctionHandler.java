/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.addressSpaceManagement;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.ReferenceViolationInterrupt;
import com.kadware.komodo.hardwarelib.BaseRegister;

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

