/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import java.util.HashSet;
import java.util.Set;

public class InstrumentedAssembler extends Assembler {

    private static final String[] dummySource = { "." };
    private static Set<AssemblerOption> noOptions = new HashSet<>();

    public InstrumentedAssembler() {
        super("TEST", dummySource, noOptions, System.out);
    }

    @Override
    public TextLine getTopLevelTextLine() {
        return new TextLine(new LineSpecifier(0, 1), dummySource[0]);
    }
}
