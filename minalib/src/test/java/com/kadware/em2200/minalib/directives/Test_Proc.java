/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.Assembler;
import com.kadware.em2200.minalib.RelocatableModule;
import org.junit.Test;

public class Test_Proc {
    @Test
    public void simpleProc(
    ) {
        String[] source = {
            "          $BASIC",
            "FOO       $PROC",
            "          LA,U      A0,5",
            "          SA        A0,R5",
            "          $END",
            "",
            "$(1),START*",
            "          HALT 0",
            "          FOO",
            "          FOO",
            "          FOO",
            "          HALT 0",
        };

        Assembler.Option[] optionSet = {
            Assembler.Option.EMIT_MODULE_SUMMARY,
            Assembler.Option.EMIT_SOURCE,
            Assembler.Option.EMIT_GENERATED_CODE,
            Assembler.Option.EMIT_DICTIONARY,
        };

        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);
    }
}
