/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

/**
 * Store Fieldata Zeros instruction
 * (SFZ) stores fieldata zeros to the operand location under j-field control
 */
public class SFZFunction extends StoreConstantFunction {

    public static final SFZFunction INSTANCE = new SFZFunction();

    private SFZFunction() {
        super("SFZ", 05, 0_060606_060606L);
    }
}
