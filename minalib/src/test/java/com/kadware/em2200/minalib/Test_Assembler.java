/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */
package com.kadware.em2200.minalib;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Assembler class in this package
 */
public class Test_Assembler {

    @Test
    public void noSource(
    ) {
        String[] source = {};

        Assembler asm = new Assembler("Test", source);
        asm.assemble();
        assertTrue(asm.getDiagnostics().isEmpty());
        assertEquals(0, asm.getParsedCode().length);
    }

    @Test
    public void whiteSpace(
    ) {
        String[] source = {
            "",
            ". This is whitespace only source code",
            "",
            ". Blah blah blah",
        };

        Assembler asm = new Assembler("Test", source);
        asm.assemble();
        assertTrue(asm.getDiagnostics().isEmpty());
        TextLine[] parsedCode = asm.getParsedCode();
        assertEquals(4, parsedCode.length);
        for (TextLine tl : parsedCode) {
            assertEquals(0, tl.getFieldCount());
        }
    }
}
