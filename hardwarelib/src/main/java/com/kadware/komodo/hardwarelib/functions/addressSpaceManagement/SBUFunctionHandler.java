/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.addressSpaceManagement;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.DesignatorRegister;

/**
 * Handles the SBU instruction f=075 j=02
 */
public class SBUFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DesignatorRegister dr = ip.getDesignatorRegister();
        if (dr.getBasicModeEnabled() && (dr.getProcessorPrivilege() > 0)) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        //  For f.a == 0, take L,BDI from PAR and offset is zero.
        //  For others, take L,BDI,offset from active base table.
        long operand;
        int brIndex = (int) iw.getA();
        if (brIndex == 0) {
            operand = ip.getProgramAddressRegister().get() & 0_777777_000000L;
        } else {
            operand = ip.getActiveBaseTableEntries()[brIndex - 1]._value;
        }

        ip.storeOperand(false, true, false, false, operand);
    }

    @Override
    public Instruction getInstruction() { return Instruction.SBU; }
}
