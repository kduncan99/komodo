/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.kex.kasm.Assembler;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class Test_Proc {

    private static final Assembler.Option[] OPTIONS = {
        Assembler.Option.EMIT_MODULE_SUMMARY,
        Assembler.Option.EMIT_SOURCE,
        Assembler.Option.EMIT_GENERATED_CODE,
        Assembler.Option.EMIT_DICTIONARY,
        };
    private static final Set<Assembler.Option> OPTION_SET = new HashSet<>(Arrays.asList(OPTIONS));

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

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setOptions(OPTION_SET)
                                               .setSource(source)
                                               .build();
        Assembler.Result result = asm.assemble();
        Assert.assertNotNull(result._relocatableModule);
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

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setOptions(OPTION_SET)
                                               .setSource(source)
                                               .build();
        Assembler.Result result = asm.assemble();
        Assert.assertNotNull(result._relocatableModule);
        //TODO check values
    }
}
