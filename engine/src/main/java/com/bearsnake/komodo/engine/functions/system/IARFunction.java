/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.system;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.HaltCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Initiate Auto-Recovery instruction
 * (IAR) Causes a halt with a reason code indicating IAR.
 * The operand is ignored.
 */
public class IARFunction extends Function {

    public static final IARFunction INSTANCE = new IARFunction();

    private IARFunction() {
        super("IAR");
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_17).setAField(0_06).setProcessorPrivilege(0));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        engine.halt(HaltCode.INITIATE_AUTO_RECOVERY);
        return true;
    }
}
