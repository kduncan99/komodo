/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
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

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

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

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

        assertTrue(asm.getDiagnostics().isEmpty());
        TextLine[] parsedCode = asm.getParsedCode();
        assertEquals(4, parsedCode.length);
        for (TextLine tl : parsedCode) {
            assertEquals(0, tl._fields.size());
        }
    }

    @Test
    public void labelFieldEmpty(
    ) {
        String[] source = {
            "  LA,U A0,0"
        };

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

        assertTrue(asm.getDiagnostics().isEmpty());
    }

    @Test
    public void labelFieldLabel(
    ) {
        String[] source = {
                "START*  LA,U A0,0"
        };

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

        assertTrue(asm.getDiagnostics().isEmpty());
        assertTrue(module._externalLabels.containsKey("START"));
        assertEquals(1, module._externalLabels.get("START")._undefinedReferences.length);
    }

    @Test
    public void labelFieldLCIndex(
    ) {
        String[] source = {
                "$(3)  LA,U      A0,0",
                "$(5)  LA,U      A0,1",
        };

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

        assertTrue(asm.getDiagnostics().isEmpty());
        assertEquals(2, module._storage.size());
        assertTrue(module._storage.containsKey(3));
        assertTrue(module._storage.containsKey(5));
    }

    @Test
    public void labelFieldLCIndexAndLabel(
    ) {
        String[] source = {
                "$(3),START*  LA,U      A0,0",
        };

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

        assertTrue(asm.getDiagnostics().isEmpty());
        assertEquals(1, module._storage.size());
        assertTrue(module._storage.containsKey(3));
        assertTrue(module._externalLabels.containsKey("START"));
        assertEquals(1, module._externalLabels.get("START")._undefinedReferences.length);
    }

    @Test
    public void labelFieldExtra1(
    ) {
        String[] source = {
                "$%$%$ LA  A5,0",
        };

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

        assertFalse(asm.getDiagnostics().isEmpty());
    }

    @Test
    public void labelFieldExtra2(
    ) {
        String[] source = {
                "$(25),%%% LA  A5,0",
        };

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

        assertFalse(asm.getDiagnostics().isEmpty());
    }

    @Test
    public void labelFieldExtra3(
    ) {
        String[] source = {
                "LABEL,EXTRA LA  A5,0",
        };

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

        assertFalse(asm.getDiagnostics().isEmpty());
    }

    @Test
    public void labelFieldExtra4(
    ) {
        String[] source = {
                "$(25),LABEL,EXTRA LA  A5,0",
        };

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

        assertFalse(asm.getDiagnostics().isEmpty());
    }

    @Test
    public void group(
    ) {
        String[] source = {
            "$(1)  + (5 + 3)*2"
        };

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

        assertTrue(asm.getDiagnostics().isEmpty());
        assertEquals(1, module._storage.size());
        assertEquals(1, module._storage.get(1)._storage.length);
        assertEquals(16, module._storage.get(1)._storage[0].getW());
    }

    @Test
    public void lit1(
    ) {
        String[] source = {
                "$(1)  + 15",
                "      + (077, 0777)"
        };

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

        assertTrue(asm.getDiagnostics().isEmpty());
        assertEquals(2, module._storage.size());
    }

    @Test
    public void lit2(
    ) {
        String[] source = {
            "$(1)  + 15",
            "      + ((077000777) + 5)"
        };

        Assembler.Option[] optionSet = { Assembler.Option.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", source, optionSet);

        assertTrue(asm.getDiagnostics().isEmpty());
        assertEquals(2, module._storage.size());
    }
}
