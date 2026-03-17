/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

/**
 * Store ASCII Spaces instruction
 * (SAS) stores ASCII spaces to the operand location under j-field control
 */
public class SASFunction extends StoreConstantFunction {

    public static final SASFunction INSTANCE = new SASFunction();

    private SASFunction() {
        super("SAS", 06, 0_040040_040040L);
    }
}
