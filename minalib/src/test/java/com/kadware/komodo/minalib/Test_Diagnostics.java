/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

import com.kadware.komodo.minalib.diagnostics.Diagnostic;
import com.kadware.komodo.minalib.diagnostics.Diagnostics;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Assembler class in this package
 */
public class Test_Diagnostics {

    private final Assembler.Option[] OPTIONS = {
        Assembler.Option.EMIT_MODULE_SUMMARY,
        Assembler.Option.EMIT_GENERATED_CODE,
        Assembler.Option.EMIT_SOURCE,
        Assembler.Option.EMIT_DICTIONARY,
    };

    @Test
    public void formDiag1(
    ) {
        String[] source = {
            "F1       $FORM     12,12,12,12",
        };

        Assembler asm = new Assembler();
        asm.assemble("TEST", source, OPTIONS);

        assertFalse(asm.getDiagnostics().isEmpty());
        Diagnostics diags = asm.getDiagnostics();
        assertFalse(diags.hasFatal());
        assertEquals(1, (int)diags.getCounters().get(Diagnostic.Level.Form));
    }

    @Test
    public void formDiag2(
    ) {
        String[] source = {
            "F1       $FORM     12,12,12",
            "         F1        1,2,3,4",
        };

        Assembler asm = new Assembler();
        asm.assemble("TEST", source, OPTIONS);

        assertFalse(asm.getDiagnostics().isEmpty());
        Diagnostics diags = asm.getDiagnostics();
        assertFalse(diags.hasFatal());
        assertEquals(1, (int)diags.getCounters().get(Diagnostic.Level.Form));
    }

    @Test
    public void formOK(
    ) {
        String[] source = {
            "F1       $FORM     12,12,12",
            "         -2",
            "         3-5",
            "         F1        1,-2,3",
        };

        Assembler asm = new Assembler();
        asm.assemble("TEST", source, OPTIONS);
    }

}
