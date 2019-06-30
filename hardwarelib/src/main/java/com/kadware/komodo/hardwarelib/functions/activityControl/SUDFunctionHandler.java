/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.activityControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the SUD instruction f=073 j=017 a=05
 */
public class SUDFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        ip.storeOperand(true,
                        true,
                        false,
                        false,
                        ip.getDesignatorRegister().getW() & 0777777);
    }

    @Override
    public Instruction getInstruction() { return Instruction.SUD; }
}
