/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.directives;

import com.kadware.komodo.minalib.Assembler;
import com.kadware.komodo.minalib.LocationCounterPool;
import com.kadware.komodo.minalib.RelocatableModule;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_Form {

    @Test
    public void simple(
    ) {
        String[] source = {
            "          $BASIC",
            "FOO       $FORM          9,9,9,6,3",
            "$(0) .",
            "          FOO 5,6,7,010,0",
        };

        Assembler.Option[] optionSet = {
            Assembler.Option.EMIT_MODULE_SUMMARY,
            Assembler.Option.EMIT_SOURCE,
            Assembler.Option.EMIT_GENERATED_CODE,
            Assembler.Option.EMIT_DICTIONARY,
        };

        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);
        assertTrue(asm.getDiagnostics().isEmpty());
        LocationCounterPool lcPool = module._storage.get(0);
        assertNotNull(lcPool);
        assertEquals(1, lcPool._storage.length);
        assertEquals(0_005_006_007_10_0L, lcPool._storage[0].getW());
    }
}
