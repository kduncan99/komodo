/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.actControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load Designator Register function
 * (LD) Loads the contents of the designator register from the memory location indicated by U.
 */
public class LDFunction extends Function {

    public static final LDFunction INSTANCE = new LDFunction();

    private LDFunction() {
        super("LD");
        setBasicModeFunctionCode(new FunctionCode(0_73).setJField(0_15).setAField(0_14).setProcessorPrivilege(0));
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_15).setAField(0_14).setProcessorPrivilege(0));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operand = engine.getOperand(false, true, false, false, false);
        var dr = engine.getDesignatorRegister();
        dr.set(operand);
        if (dr.isBasicModeEnabled()) {
            // TODO see 4.4.6.3
        }

        return true;
    }
}
