/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

/**
 * Store Negative Zero instruction
 * (SNZ) stores negative zero to the operand location under j-field control
 */
public class SNZFunction extends StoreConstantFunction {

    public SNZFunction() {
        super("SNZ", 01, 0_777777_777777L);
    }
}
