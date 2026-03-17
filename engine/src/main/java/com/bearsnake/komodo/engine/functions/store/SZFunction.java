/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

/**
 * Store Zero instruction
 * (SZ) stores zero to the operand location under j-field control
 */
public class SZFunction extends StoreConstantFunction {

    public static final SZFunction INSTANCE = new SZFunction();

    private SZFunction() {
        super("SZ", 0, 0L);
    }
}
