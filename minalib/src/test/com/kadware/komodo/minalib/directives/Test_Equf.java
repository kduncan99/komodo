/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.directives;

import com.kadware.komodo.minalib.Assembler;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_Equf {

    @Test
    public void simple_basic(
    ) {
        String[] source = {
            "          $BASIC",
            "LABEL     $EQUF *5,*X3,S1,B12",
            //TODO generate something
        };

        Assembler.Option[] optionSet = {
            Assembler.Option.EMIT_MODULE_SUMMARY,
            Assembler.Option.EMIT_SOURCE,
            Assembler.Option.EMIT_GENERATED_CODE,
            Assembler.Option.EMIT_DICTIONARY,
        };

        Assembler asm = new Assembler();
        asm.assemble("TEST", source, optionSet);
        assertTrue(asm.getDiagnostics().isEmpty());
        //TODO test generated value
    }

    @Test
    public void simple_extended(
    ) {
        String[] source = {
            "          $EXTEND",
            "LABEL     $EQUF *5,*X3,S1,B12",
            //TODO generate something
        };

        Assembler.Option[] optionSet = {
            Assembler.Option.EMIT_MODULE_SUMMARY,
            Assembler.Option.EMIT_SOURCE,
            Assembler.Option.EMIT_GENERATED_CODE,
            Assembler.Option.EMIT_DICTIONARY,
            };

        Assembler asm = new Assembler();
        asm.assemble("TEST", source, optionSet);
        assertTrue(asm.getDiagnostics().isEmpty());
        //TODO test generated value
    }
}
