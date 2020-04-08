/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.directives;

import com.kadware.komodo.minalib.Assembler;
import com.kadware.komodo.minalib.RelocatableModule;
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
        //TODO check values
    }

    @Test
    public void procWithParameters(
    ) {
        String[] source = {
            "          $BASIC",
            "FOO       $PROC",
            "          FOO",
            "          FOO(0)",
            "          FOO(0,0)",
            "          FOO(0,1)",
            "          FOO(1)",
            "          FOO(1,0)",
            "          FOO(1,1)",
            "          FOO(2,1)",
            "          $END",
            "",
            "$(1),START*",
            "          LA,H2  A0,DATA",
            "          FOO,H2 A0,A1,A2 DATA,DATA1",
        };

        Assembler.Option[] optionSet = {
            Assembler.Option.EMIT_MODULE_SUMMARY,
            Assembler.Option.EMIT_SOURCE,
            Assembler.Option.EMIT_GENERATED_CODE,
            Assembler.Option.EMIT_DICTIONARY,
        };

        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);
        //TODO check values
    }
}
