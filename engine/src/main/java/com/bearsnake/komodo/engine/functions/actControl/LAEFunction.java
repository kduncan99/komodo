/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.actControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load Addressing Environment function
 * (LAE) Loads B1 through B15 from the 15 word packet at U.
 */
public class LAEFunction extends Function {

    public static final LAEFunction INSTANCE = new LAEFunction();

    private LAEFunction() {
        super("LAE");
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_15).setAField(0_12).setProcessorPrivilege(0));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operands = engine.getConsecutiveOperands(false, 15);
        var oldAddress = engine.getProgramAddressRegister().getCompositeValue();
        engine.bankManipulation(this, (short) 0, (short) 0, (short) 0, 0, null, 0L, operands);
        engine.createJumpHistoryEntry(oldAddress);
        return true;
    }
}
