/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

/**
 * Store Negative One instruction
 * (SN1) stores negative one to the operand location under j-field control
 */
public class SN1Function extends StoreConstantFunction {

    public SN1Function() {
        super("SN1", 03, 0_777777_777776L);
    }
}
