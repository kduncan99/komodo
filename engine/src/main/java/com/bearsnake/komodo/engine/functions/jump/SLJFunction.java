/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.jump;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Store Location and Jump instruction
 * (SLJ) Stores the next address after this instruction into U, then jumps to U+1.
 */
public class SLJFunction extends Function {

    public static final SLJFunction INSTANCE = new SLJFunction();

    private SLJFunction() {
        super("SLJ");
        var c = new FunctionCode(0_72).setJField(0_01);
        setBasicModeFunctionCode(c);

        setAFieldSemantics(AFieldSemantics.UNUSED);
        setImmediateMode(false);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        boolean result = engine.resolveRelativeAddress(false, false, false);
        if (result) {
            // Store PAR.PC + 1 in U
            var retAddr = (engine.getProgramAddressRegister().getProgramCounter() + 1) & 0_777777;
            engine.storeToCachedAddress(retAddr, false, 0, false);

            // Now jump to U + 1
            engine.jumpToCachedAddressPlusOne();
        }
        return result;
    }

    @Override
    public boolean isJumpInstruction() {
        return true;
    }
}
