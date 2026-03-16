/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.functions.store;

/**
 * Store Fieldata Spaces instruction
 * (SFS) stores fieldata spaces to the operand location under j-field control
 */
public class SFSFunction extends StoreConstantFunction {

    public SFSFunction() {
        super("SFS", 04, 0_050505_050505L);
    }
}
