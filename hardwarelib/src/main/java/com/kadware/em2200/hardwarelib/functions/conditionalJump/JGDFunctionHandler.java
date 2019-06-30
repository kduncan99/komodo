/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.conditionalJump;

import com.kadware.komodo.baselib.GeneralRegister;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the JGD instruction f=070
 */
public class JGDFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  right-most 3 bits of j-field concatenated to the 4 bits of a-field is a GRS index.
        //  If the associated register is greater than zero, we effect a conditionalJump to U.
        //  In any case, the register value is decremented by 1
        int regIndex = ((int)(iw.getJ() & 07) << 4) | (int)iw.getA();
        GeneralRegister reg = ip.getGeneralRegister(regIndex);
        if (reg.isPositive() && !reg.isZero()) {
            int counter = (int) ip.getJumpOperand(true);
            ip.setProgramCounter(counter, true);
        }

        reg.setW(OnesComplement.add36Simple(reg.getW(), 0_777777_777776l));
    }

    @Override
    public Instruction getInstruction() { return Instruction.JGD; }
}
