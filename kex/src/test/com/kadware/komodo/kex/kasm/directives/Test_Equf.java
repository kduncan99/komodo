/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.kex.RelocatableModule;
import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.AssemblerOption;
import com.kadware.komodo.kex.kasm.AssemblerResult;
import com.kadware.komodo.kex.kasm.exceptions.ParameterException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_Equf {

    private static final AssemblerOption[] OPTIONS = {
        AssemblerOption.EMIT_MODULE_SUMMARY,
        AssemblerOption.EMIT_SOURCE,
        AssemblerOption.EMIT_GENERATED_CODE,
        AssemblerOption.EMIT_DICTIONARY
    };
    private static final Set<AssemblerOption> OPTION_SET = new HashSet<>(Arrays.asList(OPTIONS));

    @Test
    public void simple_basic(
    ) throws ParameterException {
        String[] source = {
            "          $BASIC",
            "$(1)",
            "LABEL     $EQUF   *5,*X3,S1",
            "          LA      A2,LABEL"
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setOptions(OPTION_SET)
                                               .setSource(source)
                                               .build();
        AssemblerResult result = asm.assemble();
        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        RelocatableModule.RelocatablePool pool = result._relocatableModule.getLocationCounterPool(1);
        assertEquals(1, pool._content.length);
        assertEquals(0_106443_600005L, pool._content[0].getW());
    }

    @Test
    public void simple_extended(
    ) {
        String[] source = {
            "          $EXTEND",
            "LABEL     $EQUF *5,*X3,S1,B12",
            //TODO generate something
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setOptions(OPTION_SET)
                                               .setSource(source)
                                               .build();
        AssemblerResult result = asm.assemble();
        assertTrue(result._diagnostics.isEmpty());
        //TODO test generated value
    }
}
