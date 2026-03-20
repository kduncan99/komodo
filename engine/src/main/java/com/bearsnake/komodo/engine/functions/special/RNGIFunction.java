/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.special;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Random Number Generator Integer instruction
 * (RNGI) Generates a 128-bit random number and stores it in U:U+3 as
 * four 32-bit words right-justified, with the highest word in U.
 */
public class RNGIFunction extends Function {

    public static final RNGIFunction INSTANCE = new RNGIFunction();

    private RNGIFunction() {
        super("RNGI");
        setExtendedModeFunctionCode(new FunctionCode(0_37).setJField(0_04).setAField(0_05));

        setAFieldSemantics(AFieldSemantics.FUNCTION_CODE_EXTENSION);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var operands = new long[4];
        for (int wx = 0; wx < 4; wx++) {
            operands[wx] = engine.getRandom().nextInt();
        }

        engine.storeConsecutiveOperands(true, operands);
        return true;
    }
}
