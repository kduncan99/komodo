/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.system;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Initiate Auto-Recovery instruction
 * (IAR) Causes a halt with a reason code indicating IAR.
 * The operand is ignored.
 */
public class SPIDFunction extends Function {

    public static final SPIDFunction INSTANCE = new SPIDFunction();

    private SPIDFunction() {
        super("SPID");
        setBasicModeFunctionCode(new FunctionCode(0_73).setJField(0_15).setAField(0_05).setProcessorPrivilege(2));
        setExtendedModeFunctionCode(new FunctionCode(0_73).setJField(0_15).setAField(0_05).setProcessorPrivilege(2));

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

        var operands = new long[2];
        // 2-word packet, no JIT, yes RNG functions, no Data expanse
        operands[0] = 0_600000_000000L;
        if (engine.getDesignatorRegister().getProcessorPrivilege() < 2) {
            operands[0] |= engine.getUpiNumber();
        }

        operands[1] = (1L << 27) | (511L << 18);
        engine.storeConsecutiveOperandsToCachedAddress(operands);
        return true;
    }
}
