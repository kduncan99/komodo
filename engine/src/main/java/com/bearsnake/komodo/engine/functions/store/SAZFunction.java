/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

/**
 * Store ASCII Zeros instruction
 * (SAZ) stores ASCII zeros to the operand location under j-field control
 */
public class SAZFunction extends StoreConstantFunction {

    public static final SAZFunction INSTANCE = new SAZFunction();

    private SAZFunction() {
        super("SAZ", 07, 0_060060_060060L);
    }
}
