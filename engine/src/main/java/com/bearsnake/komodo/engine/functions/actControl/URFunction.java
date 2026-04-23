/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.actControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * User Return function
 * (UR) Loads a previously preserved (or constructed) activity state packet, then loads B0
 * to restore an activity which had been interrupted (or is just starting up).
 * Note - the format of the 7-word ASP packet is as follows:
 *  0 Program_Address_Register
 *  1 Designator_Register
 *  2 Indicator/Key_Register
 *  3 Quantum_Timer
 *  4 F0
 *  5 ISW0
 *  6 ISW1
 */
public class URFunction extends Function {

    public static final URFunction INSTANCE = new URFunction();

    private URFunction() {
        super("UR");
        setBasicModeFunctionCode(new FunctionCode(0_73).setJField(0_15).setAField(0_16).setProcessorPrivilege(0));
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_15).setAField(0_16).setProcessorPrivilege(0));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operands = engine.getConsecutiveOperands(false, 7);
        var oldAddress = engine.getProgramAddressRegister().getCompositeValue();
        engine.bankManipulation(this, (short) 0, (short) 0, (short) 0, 0, null, 0L, operands);
        engine.createJumpHistoryEntry(oldAddress);
        return true;
    }
}
