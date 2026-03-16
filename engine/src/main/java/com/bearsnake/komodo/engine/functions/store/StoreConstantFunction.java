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

    private final int _aField;
    private final long _constant;

    protected StoreConstantFunction(
        final String mnemonic,
        final int aField,
        final long constant
    ) {
        super(mnemonic);

        _aField = aField;
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
        return engine.storeOperand(true, true, true, true, _constant);
    }
}
