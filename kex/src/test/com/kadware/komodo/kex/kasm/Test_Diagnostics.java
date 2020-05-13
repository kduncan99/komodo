/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.kex.kasm.diagnostics.Diagnostic;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Assembler class in this package
 */
public class Test_Diagnostics {

    private static final Assembler.Option[] OPTIONS = {
        Assembler.Option.EMIT_MODULE_SUMMARY,
        Assembler.Option.EMIT_SOURCE,
        Assembler.Option.EMIT_GENERATED_CODE,
        Assembler.Option.EMIT_DICTIONARY,
        };
    private static final Set<Assembler.Option> OPTION_SET = new HashSet<>(Arrays.asList(OPTIONS));

    @Test
    public void formDiag1(
    ) {
        String[] source = {
            "F1       $FORM     12,12,12,12",
        };

        Assembler assembler = new Assembler.Builder().setOptions(OPTION_SET)
                                                     .setSource(source)
                                                     .build();
        Assembler.Result result = assembler.assemble();

        assertFalse(result._diagnostics.isEmpty());
        assertFalse(result._diagnostics.hasFatal());
        assertEquals(1, (int)result._diagnostics.getCounters().get(Diagnostic.Level.Form));
    }

    @Test
    public void formDiag2(
    ) {
        String[] source = {
            "F1       $FORM     12,12,12",
            "         F1        1,2,3,4",
        };

        Assembler assembler = new Assembler.Builder().setOptions(OPTION_SET)
                                                     .setSource(source)
                                                     .build();
        Assembler.Result result = assembler.assemble();

        assertFalse(result._diagnostics.isEmpty());
        assertFalse(result._diagnostics.hasFatal());
        assertEquals(1, (int)result._diagnostics.getCounters().get(Diagnostic.Level.Form));
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

        Assembler assembler = new Assembler.Builder().setOptions(OPTION_SET)
                                                     .setSource(source)
                                                     .build();
        Assembler.Result result = assembler.assemble();

        assertTrue(result._diagnostics.isEmpty());
    }

}
