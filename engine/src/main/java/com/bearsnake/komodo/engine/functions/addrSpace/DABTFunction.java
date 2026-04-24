/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.addrSpace;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Decelerate Active Base Table Function
 * (DABT) Decelerates the active base table into the 15 words starting with U.
 * These entries correspond to the ABTEs for base registers 1 through 15.
 * ABTEs do not exist for B0 and B16 through B31.
 */
public class DABTFunction extends Function {

    public static final DABTFunction INSTANCE = new DABTFunction();

    private DABTFunction() {
        super("DABT");
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_15).setAField(0_06).setProcessorPrivilege(1));

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        if (!engine.resolveRelativeAddress(false, false, false)) {
            return false;
        }

        var operands = new long[15];
        for (int brx = 1, ox = 0; brx <= 15; brx++, ox++) {
            operands[ox] = engine.getActiveBaseTableEntry(brx).toComposite();
        }

        engine.storeConsecutiveOperandsToCachedAddress(operands);
        return true;
    }
}
