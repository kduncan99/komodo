/*
 * Copyright (c) 2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

public class FunctionAssembler extends SubAssembler {

    FunctionAssembler(
        final Assembler outerLevel,
        final String subModuleName,
        final TextLine[] sourceLines
    ) {
        super(outerLevel, subModuleName, sourceLines);
    }

    @Override
    public boolean isFunctionSubAssembly() {
        return true;
    }

    @Override
    public boolean isProcedureSubAssembly() {
        return false;
    }
}
