/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.test;

import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.InstructionPoint;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Unlock function
 * (UNLK) Ensures the Test and Set lock is cleared (by setting S1 of U to zero)
 * This instruction is executed under storage lock for U.
 */
public class UNLKFunction extends Function {

    public static final UNLKFunction INSTANCE = new UNLKFunction();

    private UNLKFunction() {
        super("UNLK");
        var fc = new FunctionCode(073).setJField(014).setAField(004);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.FUNCTION_CODE_EXTENSION);
        setImmediateMode(false);
        setIsGRS(false);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        engine.getOperand(false, false, false, false, true);
        if (engine.spGetInstructionPoint() == InstructionPoint.RESOLVING_ADDRESS) {
            return false;
        }

        engine.storeToCachedAddress(0, false, Constants.JFIELD_S1, false);
        engine.addressClearAllLocks();
        return true;
    }
}
