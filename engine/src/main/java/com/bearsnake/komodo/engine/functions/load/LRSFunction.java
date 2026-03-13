/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.load;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Load Register Set instruction
 * (LRS) Loads the GRS (or one or two subsets thereof) from the contents of U through U+n.
 * Specifically, the instruction defines two sets of ranges and lengths as follows:
 *      Aa[2:8]   = range 2 length
 *      Aa[11:17] = range 2 first GRS index
 *      Aa[20:26] = range 1 count
 *      Aa[29:35] = range 1 first GRS index
 * So we start loading registers from GRS index of range 1, for the number of registers in range 1 count,
 * from U[0] to U[range1count - 1], and then from GRS index of range 2, for the number of registers in range 2 count,
 * from U[range1count] to U[range1count + range2count - 1].
 * If either count is zero, then the associated range is not used.
 * If the GRS address exceeds 0177, it wraps around to zero.
 */
public class LRSFunction extends Function {

    public LRSFunction() {
        super("LRS");
        var fc = new FunctionCode(0_72).setJField(0_17);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.A_REGISTER);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        // TODO
        return false;
    }
}
