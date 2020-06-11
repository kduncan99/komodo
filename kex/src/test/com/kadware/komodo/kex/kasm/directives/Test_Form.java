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

public class Test_Form {

    private static final AssemblerOption[] OPTIONS = {
        AssemblerOption.EMIT_MODULE_SUMMARY,
        AssemblerOption.EMIT_SOURCE,
        AssemblerOption.EMIT_GENERATED_CODE,
        AssemblerOption.EMIT_DICTIONARY,
        };
    private static final Set<AssemblerOption> OPTION_SET = new HashSet<>(Arrays.asList(OPTIONS));

    @Test
    public void simple(
    ) throws ParameterException {
        String[] source = {
            "          $BASIC",
            "FOO       $FORM          9,9,9,6,3",
            "$(0) .",
            "          FOO 5,6,7,010,0",
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setOptions(OPTION_SET)
                                               .setSource(source)
                                               .build();
        AssemblerResult result = asm.assemble();
        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        RelocatableModule.RelocatablePool lcPool = result._relocatableModule.getLocationCounterPool(0);
        assertNotNull(lcPool);
        assertEquals(1, lcPool._content.length);
        assertEquals(0_005_006_007_10_0L, lcPool._content[0].getW());
    }
}
