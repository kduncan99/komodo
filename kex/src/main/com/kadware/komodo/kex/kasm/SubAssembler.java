/*
 * Copyright (c) 2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

public abstract class SubAssembler extends Assembler {

    SubAssembler(
        final Assembler outerLevel,
        final String subModuleName,
        final TextLine[] sourceLines
    ) {
        super(outerLevel, subModuleName, sourceLines);
    }

    @Override
    public boolean isMainAssembly() { return false; }

    @Override
    public boolean isSubAssembly() { return true; }

    public abstract boolean isFunctionSubAssembly();
    public abstract boolean isProcedureSubAssembly();
}
