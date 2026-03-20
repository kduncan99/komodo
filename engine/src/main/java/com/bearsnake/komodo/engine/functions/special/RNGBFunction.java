/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.special;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Random Number Generator Byte instruction
 * (RNGB) Generates a 128-bit random number and stores it in U:U+3 as
 * sixteen consecutive 8-bit bytes right-justified in successive quarter-words.
 */
public class RNGBFunction extends Function {

    public static final RNGBFunction INSTANCE = new RNGBFunction();

    private RNGBFunction() {
        super("RNGB");
        setExtendedModeFunctionCode(new FunctionCode(0_37).setJField(0_04).setAField(0_06));

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
            var r = engine.getRandom().nextInt();
            for (int qx = 0; qx < 4; qx++) {
                operands[wx] <<= 9;
                operands[wx] |= r & 0xFF;
                r >>= 8;
            }
        }

        engine.storeConsecutiveOperands(true, operands);
        return true;
    }
}
