/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.conditionalJump;

import com.kadware.em2200.baselib.IndexRegister;
import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.misc.DesignatorRegister;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the JMGI instruction f=074 j=012
 */
public class JMGIFunctionHandler extends FunctionHandler {

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
            int counter = (int)ip.getJumpOperand();
            ip.setProgramCounter(counter, true);
        }

        if (!dr.getBasicModeEnabled() || (iw.getA() != iw.getX()) || (iw.getH() == 0)) {
            xreg.incrementModifier18();
        }
    }
}
