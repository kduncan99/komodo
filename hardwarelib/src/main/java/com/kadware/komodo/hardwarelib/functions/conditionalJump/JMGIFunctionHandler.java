/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.conditionalJump;

import com.kadware.komodo.baselib.IndexRegister;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.DesignatorRegister;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the JMGI instruction f=074 j=012
 */
public class JMGIFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  If X(a).mod > 0, effect a conditionalJump to U
        //  In any case Increment X(a).mod by X(a).inc
        //  In Basic Mode if F0.h is true (U resolution x-reg incrementation) and F0.a == F0.x, we increment only once
        //  X(0) is used for X(a) if a == 0 (contrast to F0.x == 0 -> no indexing)
        //  In Extended Mode, X(a) incrementation is always 18 bits.
        DesignatorRegister dr = ip.getDesignatorRegister();
        IndexRegister xreg = ip.getExecOrUserXRegister((int)iw.getA());
        long modValue = xreg.getSignedXM();
        if (OnesComplement.isPositive36(modValue) && !OnesComplement.isZero36(modValue)) {
            int counter = (int)ip.getJumpOperand(true);
            ip.setProgramCounter(counter, true);
        }

        if (!dr.getBasicModeEnabled() || (iw.getA() != iw.getX()) || (iw.getH() == 0)) {
            xreg.incrementModifier18();
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.JMGI; }
}
