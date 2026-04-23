/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.actControl;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Store Designator Register function
 * (SD) Stores the contents of the designator register into the memory location indicated by U.
 */
public class SDFunction extends Function {

    public static final SDFunction INSTANCE = new SDFunction();

    private SDFunction() {
        super("SD");
        setBasicModeFunctionCode(new FunctionCode(0_73).setJField(0_15).setAField(0_15).setProcessorPrivilege(1));
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_15).setAField(0_15).setProcessorPrivilege(1));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        if (!engine.resolveRelativeAddress(false, true, false)) {
            return false;
        }

        // Note that set-to-zero bits do not need to be cleared, as they are never set by .getCompositeValue()
        engine.storeToCachedAddress(engine.getDesignatorRegister().getCompositeValue(),
                                    false, 0, false);
        return true;
    }
}
