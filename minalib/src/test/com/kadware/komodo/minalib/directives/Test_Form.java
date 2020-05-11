/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.directives;

import com.kadware.komodo.minalib.Assembler;
import com.kadware.komodo.minalib.LocationCounterPool;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_Form {

    private static final Assembler.Option[] OPTIONS = {
        Assembler.Option.EMIT_MODULE_SUMMARY,
        Assembler.Option.EMIT_SOURCE,
        Assembler.Option.EMIT_GENERATED_CODE,
        Assembler.Option.EMIT_DICTIONARY,
        };
    private static final Set<Assembler.Option> OPTION_SET = new HashSet<>(Arrays.asList(OPTIONS));

    @Test
    public void simple(
    ) {
        String[] source = {
            "          $BASIC",
            "FOO       $FORM          9,9,9,6,3",
            "$(0) .",
            "          FOO 5,6,7,010,0",
        };

        Assembler.Result result = Assembler.assemble("TEST", source, OPTION_SET);
        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        LocationCounterPool lcPool = result._relocatableModule._storage.get(0);
        assertNotNull(lcPool);
        assertEquals(1, lcPool._storage.length);
        assertEquals(0_005_006_007_10_0L, lcPool._storage[0].getW());
    }
}
