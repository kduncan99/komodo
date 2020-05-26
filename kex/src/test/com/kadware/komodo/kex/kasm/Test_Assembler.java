/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.kex.RelocatableModule;
import com.kadware.komodo.kex.kasm.exceptions.ParameterException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for the Assembler class in this package
 */
public class Test_Assembler {

    private static final AssemblerOption[] OPTIONS = {
        AssemblerOption.EMIT_MODULE_SUMMARY,
        AssemblerOption.EMIT_SOURCE,
        AssemblerOption.EMIT_GENERATED_CODE,
        AssemblerOption.EMIT_DICTIONARY,
        };
    private static final Set<AssemblerOption> OPTION_SET = new HashSet<>(Arrays.asList(OPTIONS));

    @Test
    public void noSource(
    ) {
        String[] source = {};

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();
        assertTrue(result._diagnostics.isEmpty());
        assertEquals(0, result._parsedCode.length);
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

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        TextLine[] parsedCode = result._parsedCode;
        assertEquals(4, parsedCode.length);
        for (TextLine tl : parsedCode) {
            assertEquals(0, tl._fields.size());
        }
    }

    @Test
    public void labelFieldEmpty(
    ) throws ParameterException {
        String[] source = {
            "  LA,U A0,5"
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertEquals(1, result._parsedCode.length);
        assertNotNull(result._relocatableModule);
        assertEquals("TEST", result._relocatableModule.getModuleName());
        RelocatableModule.RelocatableWord[] lcPool = result._relocatableModule.getLocationCounterPool(0);
        assertEquals(1, lcPool.length);
        assertEquals(0_107000_000005L, lcPool[0].getW());
    }

    @Test
    public void labelFieldLabel(
    ) throws ParameterException {
        String[] source = {
            "$(1),START*  LA,U A0,077"
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        assertEquals(1, result._parsedCode.length);
        assertEquals(1, result._relocatableModule.getEntryPoints().size());

        RelocatableModule.EntryPoint ep = result._relocatableModule.getEntryPoints().get("START");
        assertNotNull(ep);
        assertTrue(ep instanceof RelocatableModule.RelativeEntryPoint);
        RelocatableModule.RelativeEntryPoint rep = (RelocatableModule.RelativeEntryPoint) ep;
        assertEquals(0, rep._value);
        assertEquals(1, rep._locationCounterIndex);

        RelocatableModule.RelocatableWord[] lcPool = result._relocatableModule.getLocationCounterPool(1);
        assertEquals(1, lcPool.length);
        assertEquals(0_107000_000077L, lcPool[0].getW());
    }

    @Test
    public void labelFieldLCIndex(
    ) throws ParameterException {
        String[] source = {
                "$(3)  LA,U      A1,01000",
                "$(5)  LA,U      A2,01010",
                "$(5)  LA,U      A3,01020",
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        assertEquals(3, result._parsedCode.length);

        RelocatableModule.RelocatableWord[] lcPool3 = result._relocatableModule.getLocationCounterPool(3);
        assertEquals(1, lcPool3.length);
        assertEquals(0_107020_001000L, lcPool3[0].getW());
        RelocatableModule.RelocatableWord[] lcPool5 = result._relocatableModule.getLocationCounterPool(5);
        assertEquals(2, lcPool5.length);
        assertEquals(0_107040_001010L, lcPool5[0].getW());
        assertEquals(0_107060_001020L, lcPool5[1].getW());
    }

    @Test
    public void labelFieldLCIndexAndLabel(
    ) throws ParameterException {
        String[] source = {
                "$(3),START*  LA,U      A0,0",
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        assertEquals(1, result._parsedCode.length);

        RelocatableModule.RelocatableWord[] lcPool3 = result._relocatableModule.getLocationCounterPool(3);
        assertEquals(1, lcPool3.length);
        assertEquals(0_107000_000000L, lcPool3[0].getW());

        RelocatableModule.EntryPoint ep = result._relocatableModule.getEntryPoints().get("START");
        assertNotNull(ep);
        assertTrue(ep instanceof RelocatableModule.RelativeEntryPoint);
        RelocatableModule.RelativeEntryPoint rep = (RelocatableModule.RelativeEntryPoint) ep;
        assertEquals(0, rep._value);
        assertEquals(3, rep._locationCounterIndex);
    }

    @Test
    public void labelFieldExtra1(
    ) {
        String[] source = {
                "$,$,$ LA  A5,0",
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertFalse(result._diagnostics.isEmpty());
    }

    @Test
    public void labelFieldExtra2(
    ) {
        String[] source = {
                "$(25),$,$   LA  A5,0",
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertFalse(result._diagnostics.isEmpty());
    }

    @Test
    public void labelFieldExtra3(
    ) {
        String[] source = {
                "LABEL,EXTRA LA  A5,0",
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertFalse(result._diagnostics.isEmpty());
    }

    @Test
    public void labelFieldExtra4(
    ) {
        String[] source = {
                "$(25),LABEL,EXTRA LA  A5,0",
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertFalse(result._diagnostics.isEmpty());
    }

    @Test
    public void groupExpression(
    ) throws ParameterException {
        String[] source = {
            "$(1)  + (5 + 3)*2"
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);

        RelocatableModule.RelocatableWord[] lcPool3 = result._relocatableModule.getLocationCounterPool(1);
        assertEquals(1, lcPool3.length);
        assertEquals(16, lcPool3[0].getW());
    }

    @Test
    public void lit1(
    ) throws ParameterException {
        String[] source = {
            "$(1)  + 15",
            "      + (077, 0777)",
            "      + ((0111+FOO, 0111), 0111+FEE+FOE)"
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);

        RelocatableModule.RelocatableWord[] lcPool0 = result._relocatableModule.getLocationCounterPool(0);
        assertNotNull(lcPool0);
        assertEquals(3, lcPool0.length);
        assertEquals(0_000077_000777L, lcPool0[0].getW());

        RelocatableModule.RelocatableWord[] lcPool1 = result._relocatableModule.getLocationCounterPool(1);
        assertNotNull(lcPool1);
        assertEquals(3, lcPool1.length);
        assertEquals(0_017L, lcPool1[0].getW());
        assertEquals(0, lcPool1[1].getW());
        assertEquals(1, lcPool1[1]._relocatableItems.length);
    }

//    @Test
//    public void lit2(
//    ) {
//        //TODO inner value is not generated as a literal
//        String[] source = {
//            "$(1)  + 15",
//            "      + ((077000777) + 5)"
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertTrue(result._diagnostics.isEmpty());
//        assertNotNull(result._relocatableModule);
//        assertEquals(2, result._relocatableModule._storage.size());
//
//        LocationCounterPool lcp0 = result._relocatableModule._storage.get(0);
//        assertNotEquals(null, lcp0);
//        assertEquals(1, lcp0._storage.length);
//
//        //TODO bug here assertEquals(0_77000777L, lcp0._storage[0].getW());
//        //TODO and here assertEquals(5, lcp0._storage[1].getW());
//        //TODO and here assertEquals(1, lcp0._storage[1]._references.length);
//
//        LocationCounterPool lcp1 = result._relocatableModule._storage.get(1);
//        assertNotEquals(null, lcp1);
//        assertEquals(2, lcp1._storage.length);
//        assertEquals(0_017L, lcp1._storage[0].getW());
//        assertEquals(0, lcp1._storage[1].getW());
//        assertEquals(1, lcp1._storage[1]._references.length);
//    }

    @Test
    public void genFloating(
    ) throws ParameterException {
        String[] source = {
            "$(0)      . Explicit double precision",
            "          + 1.0D",
            "          + 1.875D",
            "          + 0.25D",
            "          - 1.875D",
            "$(2)      . Explicit single precision",
            "          + 1.0S",
            "          + 1.875S",
            "          + 0.25S",
            "          - 1.875S",
            "$(4)      . Default precision (should be double)",
            "          + 1.0",
            "          + 1.875",
            "          + 0.25",
            "          - 1.875",
            "          + 256.9375 .  1000000001111 = 400740..0"
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);

        RelocatableModule.RelocatableWord[] lcPool0 = result._relocatableModule.getLocationCounterPool(0);
        RelocatableModule.RelocatableWord[] lcPool2 = result._relocatableModule.getLocationCounterPool(2);
        RelocatableModule.RelocatableWord[] lcPool4 = result._relocatableModule.getLocationCounterPool(4);
        assertNotNull(lcPool0);
        assertNotNull(lcPool2);
        assertNotNull(lcPool4);

        assertEquals(8, lcPool0.length);
        assertEquals(0_2001_40000000L, lcPool0[0].getW());
        assertEquals(0_000000_000000L, lcPool0[1].getW());
        assertEquals(0_2001_74000000L, lcPool0[2].getW());
        assertEquals(0_000000_000000L, lcPool0[3].getW());
        assertEquals(0_1777_40000000L, lcPool0[4].getW());
        assertEquals(0_000000_000000L, lcPool0[5].getW());
        assertEquals(0_5776_03777777L, lcPool0[6].getW());
        assertEquals(0_777777_777777L, lcPool0[7].getW());

        assertEquals(4, lcPool2.length);
        assertEquals(0_201_400000000L, lcPool2[0].getW());
        assertEquals(0_201_740000000L, lcPool2[1].getW());
        assertEquals(0_177_400000000L, lcPool2[2].getW());
        assertEquals(0_576_037777777L, lcPool2[3].getW());

        assertEquals(10, lcPool4.length);
        assertEquals(0_2001_40000000L, lcPool4[0].getW());
        assertEquals(0_000000_000000L, lcPool4[1].getW());
        assertEquals(0_2001_74000000L, lcPool4[2].getW());
        assertEquals(0_000000_000000L, lcPool4[3].getW());
        assertEquals(0_1777_40000000L, lcPool4[4].getW());
        assertEquals(0_000000_000000L, lcPool4[5].getW());
        assertEquals(0_5776_03777777L, lcPool4[6].getW());
        assertEquals(0_777777_777777L, lcPool4[7].getW());
        assertEquals(0_2011_40074000L, lcPool4[8].getW());
        assertEquals(0_000000_000000L, lcPool4[9].getW());
    }

    @Test
    public void genASCIIStrings(
    ) throws ParameterException {
        String[] source = {
            "          $ASCII",
            "$(0)      .",
            "          'A'",
            "          'A'L",
            "          'A'R",
            "          'A'DL",
            "          'A'DR",
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);

        RelocatableModule.RelocatableWord[] lcPool0 = result._relocatableModule.getLocationCounterPool(0);
        assertNotNull(lcPool0);
        assertEquals(7, lcPool0.length);
        assertEquals(0_101040040040L, lcPool0[0].getW());
        assertEquals(0_101040040040L, lcPool0[1].getW());
        assertEquals(0_000000000101L, lcPool0[2].getW());
        assertEquals(0_101040040040L, lcPool0[3].getW());
        assertEquals(0_040040040040L, lcPool0[4].getW());
        assertEquals(0_000000000000L, lcPool0[5].getW());
        assertEquals(0_000000000101L, lcPool0[6].getW());
    }

    @Test
    public void genFieldataStrings(
    ) throws ParameterException {
        String[] source = {
            "          $FDATA",
            "$(0)      .",
            "          'A'",
            "          'A'L",
            "          'A'R",
            "          'A'DL",
            "          'A'DR",
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);

        RelocatableModule.RelocatableWord[] lcPool0 = result._relocatableModule.getLocationCounterPool(0);
        assertNotNull(lcPool0);
        assertEquals(7, lcPool0.length);
        assertEquals(0_060505050505L, lcPool0[0].getW());
        assertEquals(0_060505050505L, lcPool0[1].getW());
        assertEquals(0_000000000006L, lcPool0[2].getW());
        assertEquals(0_060505050505L, lcPool0[3].getW());
        assertEquals(0_050505050505L, lcPool0[4].getW());
        assertEquals(0_000000000000L, lcPool0[5].getW());
        assertEquals(0_000000000006L, lcPool0[6].getW());
    }
}
