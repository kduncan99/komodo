/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

/**
 * Store Positive One instruction
 * (SP1) stores positive one to the operand location under j-field control
 */
public class SP1Function extends StoreConstantFunction {

    public static final SP1Function INSTANCE = new SP1Function();

    private SP1Function() {
        super("SP1", 02, 01L);
    }
}
