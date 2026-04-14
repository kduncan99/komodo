/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

import com.bearsnake.komodo.engine.Engine;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionCode;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * Base class for all store instructions which store a constant.
 * These are characterized by f=05, j=partial-word, and a=constant indicator.
 * The code is identical, so the execution and tests are identical.
 */
public abstract class StoreConstantFunction extends Function {

    private final long _constant;

    protected StoreConstantFunction(
        final String mnemonic,
        final int aField,
        final long constant
    ) {
        super(mnemonic);

        _constant = constant;

        var fc = new FunctionCode(0_05).setAField(aField);
        setBasicModeFunctionCode(fc);
        setExtendedModeFunctionCode(fc);

        setAFieldSemantics(AFieldSemantics.FUNCTION_CODE_EXTENSION);
        setImmediateMode(true);
        setIsGRS(true);
    }

    @Override
    public boolean execute(
        final Engine engine
    ) throws MachineInterrupt {
        var result = engine.resolveRelativeAddress(false, true, false);
        if (result) {
            var ci = engine.getCurrentInstruction();
            engine.storeToCachedAddress(_constant, false, ci.getJ(), false);
        }
        return result;
    }
}
