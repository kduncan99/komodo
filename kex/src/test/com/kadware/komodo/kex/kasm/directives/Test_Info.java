/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.AssemblerOption;
import com.kadware.komodo.kex.kasm.AssemblerResult;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class Test_Info {

    private static final AssemblerOption[] OPTIONS = {
        AssemblerOption.EMIT_MODULE_SUMMARY,
        AssemblerOption.EMIT_SOURCE,
        AssemblerOption.EMIT_GENERATED_CODE,
        AssemblerOption.EMIT_DICTIONARY,
        };
    private static final Set<AssemblerOption> OPTION_SET = new HashSet<>(Arrays.asList(OPTIONS));


    @Test
    public void simple_10_good() {
        String[] source = {
            "          $BASIC",
            "          $INFO 10 1, 2, 3",
            "          $END",
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setOptions(OPTION_SET)
                                               .setSource(source)
                                               .build();
        AssemblerResult result = asm.assemble();
        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        Set<Integer> lcIndices = result._relocatableModule.getEstablishedLocationCounterIndices();
        assertEquals(3, lcIndices.size());
        assertTrue(lcIndices.contains(1));
        assertTrue(lcIndices.contains(2));
        assertTrue(lcIndices.contains(3));
    }

    @Test
    public void simple_10_bad_operand() {
        String[] source = {
            "          $BASIC",
            "          $INFO 10 'goober'",
            "          $END",
            };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setOptions(OPTION_SET)
                                               .setSource(source)
                                               .build();
        AssemblerResult result = asm.assemble();
        assertTrue(result._diagnostics.hasError());
        assertNotNull(result._relocatableModule);
        Set<Integer> lcIndices = result._relocatableModule.getEstablishedLocationCounterIndices();
        assertEquals(0, lcIndices.size());
    }

    @Test
    public void simple_10_no_operand() {
        String[] source = {
            "          $BASIC",
            "          $INFO 10",
            "          $END",
            };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setOptions(OPTION_SET)
                                               .setSource(source)
                                               .build();
        AssemblerResult result = asm.assemble();
        assertTrue(result._diagnostics.hasError());
        assertNotNull(result._relocatableModule);
        Set<Integer> lcIndices = result._relocatableModule.getEstablishedLocationCounterIndices();
        assertEquals(0, lcIndices.size());
    }

    //  TODO Need many more $INFO tests
}
