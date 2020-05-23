/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

//    @Test
//    public void noSource(
//    ) {
//        String[] source = {};
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertTrue(result._diagnostics.isEmpty());
//        assertEquals(0, result._parsedCode.length);
//    }

//    @Test
//    public void whiteSpace(
//    ) {
//        String[] source = {
//            "",
//            ". This is whitespace only source code",
//            "",
//            ". Blah blah blah",
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertTrue(result._diagnostics.isEmpty());
//        TextLine[] parsedCode = result._parsedCode;
//        assertEquals(4, parsedCode.length);
//        for (TextLine tl : parsedCode) {
//            assertEquals(0, tl._fields.size());
//        }
//    }

//    @Test
//    public void labelFieldEmpty(
//    ) {
//        String[] source = {
//            "  LA,U A0,0"
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertTrue(result._diagnostics.isEmpty());
//    }

//    @Test
//    public void labelFieldLabel(
//    ) {
//        String[] source = {
//                "START*  LA,U A0,0"
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertTrue(result._diagnostics.isEmpty());
//        assertNotNull(result._relocatableModule);
//        assertTrue(result._relocatableModule._externalLabels.containsKey("START"));
//        assertEquals(1, result._relocatableModule._externalLabels.get("START")._references.length);
//    }

//    @Test
//    public void labelFieldLCIndex(
//    ) {
//        String[] source = {
//                "$(3)  LA,U      A0,0",
//                "$(5)  LA,U      A0,1",
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertTrue(result._diagnostics.isEmpty());
//        assertNotNull(result._relocatableModule);
//        assertEquals(2, result._relocatableModule._storage.size());
//        assertTrue(result._relocatableModule._storage.containsKey(3));
//        assertTrue(result._relocatableModule._storage.containsKey(5));
//    }

//    @Test
//    public void labelFieldLCIndexAndLabel(
//    ) {
//        String[] source = {
//                "$(3),START*  LA,U      A0,0",
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertTrue(result._diagnostics.isEmpty());
//        assertNotNull(result._relocatableModule);
//        assertEquals(1, result._relocatableModule._storage.size());
//        assertTrue(result._relocatableModule._storage.containsKey(3));
//        assertTrue(result._relocatableModule._externalLabels.containsKey("START"));
//        assertEquals(1, result._relocatableModule._externalLabels.get("START")._references.length);
//    }

//    @Test
//    public void labelFieldExtra1(
//    ) {
//        String[] source = {
//                "$%$%$ LA  A5,0",
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertFalse(result._diagnostics.isEmpty());
//    }

//    @Test
//    public void labelFieldExtra2(
//    ) {
//        String[] source = {
//                "$(25),%%% LA  A5,0",
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertFalse(result._diagnostics.isEmpty());
//    }

//    @Test
//    public void labelFieldExtra3(
//    ) {
//        String[] source = {
//                "LABEL,EXTRA LA  A5,0",
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertFalse(result._diagnostics.isEmpty());
//    }

//    @Test
//    public void labelFieldExtra4(
//    ) {
//        String[] source = {
//                "$(25),LABEL,EXTRA LA  A5,0",
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertFalse(result._diagnostics.isEmpty());
//    }

//    @Test
//    public void group(
//    ) {
//        String[] source = {
//            "$(1)  + (5 + 3)*2"
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertTrue(result._diagnostics.isEmpty());
//        assertNotNull(result._relocatableModule);
//        assertEquals(1, result._relocatableModule._storage.size());
//        assertEquals(1, result._relocatableModule._storage.get(1)._storage.length);
//        assertEquals(16, result._relocatableModule._storage.get(1)._storage[0].getW());
//    }

//    @Test
//    public void lit1(
//    ) {
//        String[] source = {
//            "$(1)  + 15",
//            "      + (077, 0777)",
//            "      + ((0111+FOO, 0111), 0111+FEE+FOE)"
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertTrue(result._diagnostics.isEmpty());
//        assertNotNull(result._relocatableModule);
//        assertEquals(2, result._relocatableModule._storage.size());
//
//        LocationCounterPool lcp0 = result._relocatableModule._storage.get(0);
//        assertNotEquals(null, lcp0);
//        assertEquals(3, lcp0._storage.length);
//        assertEquals(0_000077_000777L, lcp0._storage[0].getW());
//
//        LocationCounterPool lcp1 = result._relocatableModule._storage.get(1);
//        assertNotEquals(null, lcp1);
//        assertEquals(3, lcp1._storage.length);
//        assertEquals(0_017L, lcp1._storage[0].getW());
//        assertEquals(0, lcp1._storage[1].getW());
//        assertEquals(1, lcp1._storage[1]._references.length);
//    }

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

//    @Test
//    public void genFloating(
//    ) {
//        String[] source = {
//            "$(0)      . Explicit double precision",
//            "          + 1.0D",
//            "          + 1.875D",
//            "          + 0.25D",
//            "          - 1.875D",
//            "$(2)      . Explicit single precision",
//            "          + 1.0S",
//            "          + 1.875S",
//            "          + 0.25S",
//            "          - 1.875S",
//            "$(4)      . Default precision (should be double)",
//            "          + 1.0",
//            "          + 1.875",
//            "          + 0.25",
//            "          - 1.875",
//            "          + 256.9375 .  1000000001111 = 400740..0"
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertTrue(result._diagnostics.isEmpty());
//        assertNotNull(result._relocatableModule);
//
//        LocationCounterPool lcp0 = result._relocatableModule._storage.get(0);
//        assertNotNull(lcp0);
//        assertEquals(8, lcp0._storage.length);
//        assertEquals(0_2001_40000000L, lcp0._storage[0].getW());
//        assertEquals(0_000000_000000L, lcp0._storage[1].getW());
//        assertEquals(0_2001_74000000L, lcp0._storage[2].getW());
//        assertEquals(0_000000_000000L, lcp0._storage[3].getW());
//        assertEquals(0_1777_40000000L, lcp0._storage[4].getW());
//        assertEquals(0_000000_000000L, lcp0._storage[5].getW());
//        assertEquals(0_5776_03777777L, lcp0._storage[6].getW());
//        assertEquals(0_777777_777777L, lcp0._storage[7].getW());
//
//        LocationCounterPool lcp2 = result._relocatableModule._storage.get(2);
//        assertNotNull(lcp2);
//        assertEquals(4, lcp2._storage.length);
//        assertEquals(0_201_400000000L, lcp2._storage[0].getW());
//        assertEquals(0_201_740000000L, lcp2._storage[1].getW());
//        assertEquals(0_177_400000000L, lcp2._storage[2].getW());
//        assertEquals(0_576_037777777L, lcp2._storage[3].getW());
//
//        LocationCounterPool lcp4 = result._relocatableModule._storage.get(4);
//        assertNotNull(lcp4);
//        assertEquals(10, lcp4._storage.length);
//        assertEquals(0_2001_40000000L, lcp4._storage[0].getW());
//        assertEquals(0_000000_000000L, lcp4._storage[1].getW());
//        assertEquals(0_2001_74000000L, lcp4._storage[2].getW());
//        assertEquals(0_000000_000000L, lcp4._storage[3].getW());
//        assertEquals(0_1777_40000000L, lcp4._storage[4].getW());
//        assertEquals(0_000000_000000L, lcp4._storage[5].getW());
//        assertEquals(0_5776_03777777L, lcp4._storage[6].getW());
//        assertEquals(0_777777_777777L, lcp4._storage[7].getW());
//        assertEquals(0_2011_40074000L, lcp4._storage[8].getW());
//        assertEquals(0_000000_000000L, lcp4._storage[9].getW());
//    }

//    @Test
//    public void genASCIIStrings(
//    ) {
//        String[] source = {
//            "          $ASCII",
//            "$(0)      .",
//            "          'A'",
//            "          'A'L",
//            "          'A'R",
//            "          'A'DL",
//            "          'A'DR",
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertTrue(result._diagnostics.isEmpty());
//        assertNotNull(result._relocatableModule);
//
//        LocationCounterPool lcp0 = result._relocatableModule._storage.get(0);
//        assertNotEquals(null, lcp0);
//        assertEquals(7, lcp0._storage.length);
//        assertEquals(0_101040040040L, lcp0._storage[0].getW());
//        assertEquals(0_101040040040L, lcp0._storage[1].getW());
//        assertEquals(0_000000000101L, lcp0._storage[2].getW());
//        assertEquals(0_101040040040L, lcp0._storage[3].getW());
//        assertEquals(0_040040040040L, lcp0._storage[4].getW());
//        assertEquals(0_000000000000L, lcp0._storage[5].getW());
//        assertEquals(0_000000000101L, lcp0._storage[6].getW());
//    }

//    @Test
//    public void genFieldataStrings(
//    ) {
//        String[] source = {
//            "          $FDATA",
//            "$(0)      .",
//            "          'A'",
//            "          'A'L",
//            "          'A'R",
//            "          'A'DL",
//            "          'A'DR",
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TEST", source, OPTION_SET);
//        assertTrue(result._diagnostics.isEmpty());
//        assertNotNull(result._relocatableModule);
//
//        LocationCounterPool lcp0 = result._relocatableModule._storage.get(0);
//        assertNotEquals(null, lcp0);
//        assertEquals(7, lcp0._storage.length);
//        assertEquals(0_060505050505L, lcp0._storage[0].getW());
//        assertEquals(0_060505050505L, lcp0._storage[1].getW());
//        assertEquals(0_000000000006L, lcp0._storage[2].getW());
//        assertEquals(0_060505050505L, lcp0._storage[3].getW());
//        assertEquals(0_050505050505L, lcp0._storage[4].getW());
//        assertEquals(0_000000000000L, lcp0._storage[5].getW());
//        assertEquals(0_000000000006L, lcp0._storage[6].getW());
//    }
}
