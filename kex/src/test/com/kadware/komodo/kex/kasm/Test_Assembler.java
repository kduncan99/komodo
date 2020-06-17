/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.baselib.FieldDescriptor;
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
        AssemblerOption.EMIT_DICTIONARY
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
        RelocatableModule.RelocatablePool lcPool = result._relocatableModule.getLocationCounterPool(0);
        assertEquals(1, lcPool._content.length);
        assertEquals(0_107000_000005L, lcPool._content[0].getW());
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

        RelocatableModule.RelocatablePool lcPool = result._relocatableModule.getLocationCounterPool(1);
        assertEquals(1, lcPool._content.length);
        assertEquals(0_107000_000077L, lcPool._content[0].getW());
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

        RelocatableModule.RelocatablePool lcPool3 = result._relocatableModule.getLocationCounterPool(3);
        assertEquals(1, lcPool3._content.length);
        assertEquals(0_107020_001000L, lcPool3._content[0].getW());
        RelocatableModule.RelocatablePool lcPool5 = result._relocatableModule.getLocationCounterPool(5);
        assertEquals(2, lcPool5._content.length);
        assertEquals(0_107040_001010L, lcPool5._content[0].getW());
        assertEquals(0_107060_001020L, lcPool5._content[1].getW());
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

        RelocatableModule.RelocatablePool lcPool3 = result._relocatableModule.getLocationCounterPool(3);
        assertEquals(1, lcPool3._content.length);
        assertEquals(0_107000_000000L, lcPool3._content[0].getW());

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
    public void labelFieldLong_Good(
    ) throws ParameterException {
        String[] source = {
            "$(1),THISLABELISNOTTOOLONG*  LA,U A0,077"
        };

        Set<AssemblerOption> extraOption = new HashSet<>(OPTION_SET);
        extraOption.add(AssemblerOption.LONG_IDENTIFIERS);
        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(extraOption)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        assertEquals(1, result._parsedCode.length);
        assertEquals(1, result._relocatableModule.getEntryPoints().size());

        RelocatableModule.EntryPoint ep = result._relocatableModule.getEntryPoints().get("THISLABELISNOTTOOLONG");
        assertNotNull(ep);
        assertTrue(ep instanceof RelocatableModule.RelativeEntryPoint);
        RelocatableModule.RelativeEntryPoint rep = (RelocatableModule.RelativeEntryPoint) ep;
        assertEquals(0, rep._value);
        assertEquals(1, rep._locationCounterIndex);

        RelocatableModule.RelocatablePool lcPool = result._relocatableModule.getLocationCounterPool(1);
        assertEquals(1, lcPool._content.length);
        assertEquals(0_107000_000077L, lcPool._content[0].getW());
    }

    @Test
    public void labelFieldLong_Bad(
    ) {
        String[] source = {
            "$(1),THISLABELISTOOLONG*  LA,U A0,077"
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertEquals(1, result._diagnostics.getDiagnostics().size());
        assertNotNull(result._relocatableModule);
        assertEquals(1, result._parsedCode.length);
        assertEquals(0, result._relocatableModule.getEntryPoints().size());
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

        RelocatableModule.RelocatablePool lcPool3 = result._relocatableModule.getLocationCounterPool(1);
        assertEquals(1, lcPool3._content.length);
        assertEquals(16, lcPool3._content[0].getW());
    }

    @Test
    public void lit1(
    ) throws ParameterException {
        String[] source = {
            "$(0)  $LIT",
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

        RelocatableModule.RelocatablePool lcPool0 = result._relocatableModule.getLocationCounterPool(0);
        assertNotNull(lcPool0);
        assertEquals(3, lcPool0._content.length);
        assertEquals(0_000077_000777L, lcPool0._content[0].getW());

        RelocatableModule.RelocatablePool lcPool1 = result._relocatableModule.getLocationCounterPool(1);
        assertNotNull(lcPool1);
        assertEquals(3, lcPool1._content.length);
        assertEquals(0_017L, lcPool1._content[0].getW());
        assertEquals(0, lcPool1._content[1].getW());
        assertEquals(1, lcPool1._content[1]._relocatableItems.length);
    }


    @Test
    public void lit2(
    ) throws ParameterException {
        String[] source = {
            "$(0)  $LIT",
            "$(1)  + 0777000777",
            "$(2)  $LIT",
            "$(3)  + (0777000777)",
            "$(4)  $LIT",
            "$(5)  + ((0777000777))",
            "$(6)  $LIT",
            "$(7)  + (((0777000777)))"
        };
        //  Expected:
        //  $(0) empty
        //  $(1) 000000: 000777000777
        //  $(2) 000000: 000777000777
        //  $(3) 000000: 000000000000[0:35]$(2)
        //  $(4) 000000: 000777000777
        //       000000: 000000000000[0:35]$(4)
        //  $(5) 000000: 000000000001[0:35]$(4)
        //  $(6) 000000: 000777000777
        //       000001: 000000000000[0:35]$(6)
        //       000002: 000000000001[0:35]$(6)
        //  $(7) 000000: 000000000002[0:35]$(6)

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        Set<Integer> lcIndices = result._relocatableModule.getEstablishedLocationCounterIndices();
        assertEquals(7, lcIndices.size());

        {
            RelocatableModule.RelocatablePool lcPool1 = result._relocatableModule.getLocationCounterPool(1);
            assertEquals(1, lcPool1._content.length);
            assertEquals(0_777000777L, lcPool1._content[0].getW());
        }

        {
            RelocatableModule.RelocatablePool lcPool2 = result._relocatableModule.getLocationCounterPool(2);
            assertEquals(1, lcPool2._content.length);
            assertEquals(0_777000777L, lcPool2._content[0].getW());

            RelocatableModule.RelocatablePool lcPool3 = result._relocatableModule.getLocationCounterPool(3);
            assertEquals(1, lcPool3._content.length);
            assertEquals(0L, lcPool3._content[0].getW());
            assertEquals(1, lcPool3._content[0]._relocatableItems.length);
            RelocatableModule.RelocatableItem ri = lcPool3._content[0]._relocatableItems[0];
            assertTrue(ri instanceof RelocatableModule.RelocatableItemLocationCounter);
            RelocatableModule.RelocatableItemLocationCounter rilc = (RelocatableModule.RelocatableItemLocationCounter) ri;
            assertEquals(new FieldDescriptor(0, 36), rilc._fieldDescriptor);
            assertEquals(2, rilc._locationCounterIndex);
            assertFalse(rilc._subtraction);
        }

        {
            RelocatableModule.RelocatablePool lcPool4 = result._relocatableModule.getLocationCounterPool(4);
            assertEquals(2, lcPool4._content.length);
            assertEquals(0_777000777L, lcPool4._content[0].getW());
            assertEquals(0L, lcPool4._content[1].getW());
            RelocatableModule.RelocatableItem ri1 = lcPool4._content[1]._relocatableItems[0];
            assertTrue(ri1 instanceof RelocatableModule.RelocatableItemLocationCounter);
            RelocatableModule.RelocatableItemLocationCounter ri1lc = (RelocatableModule.RelocatableItemLocationCounter) ri1;
            assertEquals(4, ri1lc._locationCounterIndex);

            RelocatableModule.RelocatablePool lcPool5 = result._relocatableModule.getLocationCounterPool(5);
            assertEquals(1, lcPool5._content.length);
            assertEquals(1L, lcPool5._content[0].getW());
            assertEquals(1, lcPool5._content[0]._relocatableItems.length);
            RelocatableModule.RelocatableItem ri2 = lcPool5._content[0]._relocatableItems[0];
            assertTrue(ri2 instanceof RelocatableModule.RelocatableItemLocationCounter);
            RelocatableModule.RelocatableItemLocationCounter ri2lc = (RelocatableModule.RelocatableItemLocationCounter) ri2;
            assertEquals(4, ri2lc._locationCounterIndex);
        }

        {
            RelocatableModule.RelocatablePool lcPool6 = result._relocatableModule.getLocationCounterPool(6);
            assertEquals(3, lcPool6._content.length);
            assertEquals(0_777000777L, lcPool6._content[0].getW());
            assertEquals(0, lcPool6._content[0]._relocatableItems.length);

            assertEquals(0L, lcPool6._content[1].getW());
            assertEquals(1, lcPool6._content[1]._relocatableItems.length);
            RelocatableModule.RelocatableItem ri1 = lcPool6._content[1]._relocatableItems[0];
            assertTrue(ri1 instanceof RelocatableModule.RelocatableItemLocationCounter);
            RelocatableModule.RelocatableItemLocationCounter ri1lc = (RelocatableModule.RelocatableItemLocationCounter) ri1;
            assertEquals(6, ri1lc._locationCounterIndex);

            assertEquals(1L, lcPool6._content[2].getW());
            assertEquals(1, lcPool6._content[2]._relocatableItems.length);
            RelocatableModule.RelocatableItem ri2 = lcPool6._content[1]._relocatableItems[0];
            assertTrue(ri2 instanceof RelocatableModule.RelocatableItemLocationCounter);
            RelocatableModule.RelocatableItemLocationCounter ri2lc = (RelocatableModule.RelocatableItemLocationCounter) ri2;
            assertEquals(6, ri2lc._locationCounterIndex);

            RelocatableModule.RelocatablePool lcPool7 = result._relocatableModule.getLocationCounterPool(7);
            assertEquals(1, lcPool7._content.length);
            assertEquals(2L, lcPool7._content[0].getW());
            assertEquals(1, lcPool7._content[0]._relocatableItems.length);
            RelocatableModule.RelocatableItem ri3 = lcPool7._content[0]._relocatableItems[0];
            assertTrue(ri3 instanceof RelocatableModule.RelocatableItemLocationCounter);
            RelocatableModule.RelocatableItemLocationCounter ri3lc = (RelocatableModule.RelocatableItemLocationCounter) ri2;
            assertEquals(6, ri3lc._locationCounterIndex);
        }
    }

    @Test
    public void lit3(
    ) throws ParameterException {
        String[] source = {
            "$(1)  + 15",
            "      + ((077000777) + 5)"
        };
        //  Expected:
        //  $(0) 000000: 000077000777
        //  $(1) 000000: 000000000017
        //       000001: 000000000005[0:35]$(0)

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        Set<Integer> lcIndices = result._relocatableModule.getEstablishedLocationCounterIndices();
        assertEquals(2, lcIndices.size());
        assertTrue(lcIndices.contains(0));
        assertTrue(lcIndices.contains(1));

        RelocatableModule.RelocatablePool lcPool0 = result._relocatableModule.getLocationCounterPool(0);
        assertEquals(1, lcPool0._content.length);
        assertEquals(0_77000777L, lcPool0._content[0].getW());

        RelocatableModule.RelocatablePool lcPool1 = result._relocatableModule.getLocationCounterPool(1);
        assertEquals(2, lcPool1._content.length);
        assertEquals(0_017L, lcPool1._content[0].getW());
        assertEquals(0_05L, lcPool1._content[1].getW());
        assertEquals(1, lcPool1._content[1]._relocatableItems.length);
        RelocatableModule.RelocatableItem ri = lcPool1._content[1]._relocatableItems[0];
        assertTrue(ri instanceof RelocatableModule.RelocatableItemLocationCounter);
        RelocatableModule.RelocatableItemLocationCounter rilc = (RelocatableModule.RelocatableItemLocationCounter) ri;
        assertEquals(new FieldDescriptor(0, 36), rilc._fieldDescriptor);
        assertEquals(0, rilc._locationCounterIndex);
        assertFalse(rilc._subtraction);
    }

    @Test
    public void lit4(
    ) throws ParameterException {
        String[] source = {
            "$(1)  + ((5, 013) + 5)"
        };
        //  Expected:
        //  $(0) 000000: 000005000013
        //  $(1) 000000: 000000000005[0:35]$(0)

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        Set<Integer> lcIndices = result._relocatableModule.getEstablishedLocationCounterIndices();
        assertEquals(2, lcIndices.size());
        assertTrue(lcIndices.contains(0));
        assertTrue(lcIndices.contains(1));

        RelocatableModule.RelocatablePool lcPool0 = result._relocatableModule.getLocationCounterPool(0);
        assertEquals(1, lcPool0._content.length);
        assertEquals(0_000005_000013L, lcPool0._content[0].getW());

        RelocatableModule.RelocatablePool lcPool1 = result._relocatableModule.getLocationCounterPool(1);
        assertEquals(1, lcPool1._content.length);
        assertEquals(0_05L, lcPool1._content[0].getW());
        assertEquals(1, lcPool1._content[0]._relocatableItems.length);
        RelocatableModule.RelocatableItem ri = lcPool1._content[0]._relocatableItems[0];
        assertTrue(ri instanceof RelocatableModule.RelocatableItemLocationCounter);
        RelocatableModule.RelocatableItemLocationCounter rilc = (RelocatableModule.RelocatableItemLocationCounter) ri;
        assertEquals(new FieldDescriptor(0, 36), rilc._fieldDescriptor);
        assertEquals(0, rilc._locationCounterIndex);
        assertFalse(rilc._subtraction);
    }

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

        RelocatableModule.RelocatablePool lcPool0 = result._relocatableModule.getLocationCounterPool(0);
        RelocatableModule.RelocatablePool lcPool2 = result._relocatableModule.getLocationCounterPool(2);
        RelocatableModule.RelocatablePool lcPool4 = result._relocatableModule.getLocationCounterPool(4);
        assertNotNull(lcPool0);
        assertNotNull(lcPool2);
        assertNotNull(lcPool4);

        assertEquals(8, lcPool0._content.length);
        assertEquals(0_2001_40000000L, lcPool0._content[0].getW());
        assertEquals(0_000000_000000L, lcPool0._content[1].getW());
        assertEquals(0_2001_74000000L, lcPool0._content[2].getW());
        assertEquals(0_000000_000000L, lcPool0._content[3].getW());
        assertEquals(0_1777_40000000L, lcPool0._content[4].getW());
        assertEquals(0_000000_000000L, lcPool0._content[5].getW());
        assertEquals(0_5776_03777777L, lcPool0._content[6].getW());
        assertEquals(0_777777_777777L, lcPool0._content[7].getW());

        assertEquals(4, lcPool2._content.length);
        assertEquals(0_201_400000000L, lcPool2._content[0].getW());
        assertEquals(0_201_740000000L, lcPool2._content[1].getW());
        assertEquals(0_177_400000000L, lcPool2._content[2].getW());
        assertEquals(0_576_037777777L, lcPool2._content[3].getW());

        assertEquals(10, lcPool4._content.length);
        assertEquals(0_2001_40000000L, lcPool4._content[0].getW());
        assertEquals(0_000000_000000L, lcPool4._content[1].getW());
        assertEquals(0_2001_74000000L, lcPool4._content[2].getW());
        assertEquals(0_000000_000000L, lcPool4._content[3].getW());
        assertEquals(0_1777_40000000L, lcPool4._content[4].getW());
        assertEquals(0_000000_000000L, lcPool4._content[5].getW());
        assertEquals(0_5776_03777777L, lcPool4._content[6].getW());
        assertEquals(0_777777_777777L, lcPool4._content[7].getW());
        assertEquals(0_2011_40074000L, lcPool4._content[8].getW());
        assertEquals(0_000000_000000L, lcPool4._content[9].getW());
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

        RelocatableModule.RelocatablePool lcPool0 = result._relocatableModule.getLocationCounterPool(0);
        assertNotNull(lcPool0);
        assertEquals(7, lcPool0._content.length);
        assertEquals(0_101040040040L, lcPool0._content[0].getW());
        assertEquals(0_101040040040L, lcPool0._content[1].getW());
        assertEquals(0_000000000101L, lcPool0._content[2].getW());
        assertEquals(0_101040040040L, lcPool0._content[3].getW());
        assertEquals(0_040040040040L, lcPool0._content[4].getW());
        assertEquals(0_000000000000L, lcPool0._content[5].getW());
        assertEquals(0_000000000101L, lcPool0._content[6].getW());
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

        RelocatableModule.RelocatablePool lcPool0 = result._relocatableModule.getLocationCounterPool(0);
        assertNotNull(lcPool0);
        assertEquals(7, lcPool0._content.length);
        assertEquals(0_060505050505L, lcPool0._content[0].getW());
        assertEquals(0_060505050505L, lcPool0._content[1].getW());
        assertEquals(0_000000000006L, lcPool0._content[2].getW());
        assertEquals(0_060505050505L, lcPool0._content[3].getW());
        assertEquals(0_050505050505L, lcPool0._content[4].getW());
        assertEquals(0_000000000000L, lcPool0._content[5].getW());
        assertEquals(0_000000000006L, lcPool0._content[6].getW());
    }

    @Test
    public void bField_ExecRegisters(
    ) throws ParameterException {
        String[] source = {
            "          $EXTEND",
            "$(1)      .",
            "          LA        A5,0,,B16",
            "          LA        A6,0,,B31"
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setSource(source)
                                               .setOptions(OPTION_SET)
                                               .build();
        AssemblerResult result = asm.assemble();

        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);

        RelocatableModule.RelocatablePool lcPool1 = result._relocatableModule.getLocationCounterPool(1);
        assertNotNull(lcPool1);
        assertEquals(2, lcPool1._content.length);
        assertEquals(0_100120_200000L, lcPool1._content[0].getW());
        assertEquals(0_100140_370000L, lcPool1._content[1].getW());
    }
}
