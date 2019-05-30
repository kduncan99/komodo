/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_Linker {

    @Test
    public void test_empty() {

        Linker.BankDeclaration[] bds = {
            new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 0, (short) 010))
                                                .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                                .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                                .setInitialBaseRegister(0)
                                                .setBankLevel(02)
                                                .setBankDescriptorIndex(04)
                                                .setBankName("I1")
                                                .setPoolSpecifications(new Linker.LCPoolSpecification[0])
                                                .setStartingAddress(01000)
                                                .build(),
            new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 077))
                                                .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                                .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                                .setInitialBaseRegister(2)
                                                .setBankLevel(04)
                                                .setBankDescriptorIndex(05)
                                                .setBankName("D1")
                                                .setPoolSpecifications(new Linker.LCPoolSpecification[0])
                                                .setStartingAddress(02000)
                                                .build(),
        };

        Linker.Option[] options = {
            Linker.Option.OPTION_NO_ENTRY_POINT,
            Linker.Option.OPTION_EMIT_SUMMARY,
            Linker.Option.OPTION_EMIT_DICTIONARY
        };

        Linker linker = new Linker();
        AbsoluteModule abs = linker.link("TEST_ASM", bds, options);

        assertNotNull(abs);
        assertNull(abs._entryPointAddress);
        assertEquals(2, abs._loadableBanks.size());
        assertTrue(abs._loadableBanks.containsKey(04));
        assertTrue(abs._loadableBanks.containsKey(05));

        LoadableBank ibank = abs._loadableBanks.get(04);
        assertEquals(0, ibank._accessInfo._ring);
        assertEquals(010, ibank._accessInfo._domain);
        assertFalse(ibank._generalPermissions._enter);
        assertFalse(ibank._generalPermissions._read);
        assertFalse(ibank._generalPermissions._write);
        assertTrue(ibank._specialPermissions._enter);
        assertTrue(ibank._specialPermissions._read);
        assertTrue(ibank._specialPermissions._write);
        assertEquals(2, ibank._bankLevel);
        assertEquals(0, (int) ibank._initialBaseRegister);
        assertFalse(ibank._isExtendedMode);
        assertEquals(0, ibank._content.getArraySize());
        assertEquals(01000, ibank._startingAddress);

        LoadableBank dbank = abs._loadableBanks.get(05);
        assertEquals(03, dbank._accessInfo._ring);
        assertEquals(077, dbank._accessInfo._domain);
        assertFalse(dbank._generalPermissions._enter);
        assertFalse(dbank._generalPermissions._read);
        assertFalse(dbank._generalPermissions._write);
        assertFalse(dbank._specialPermissions._enter);
        assertTrue(dbank._specialPermissions._read);
        assertTrue(dbank._specialPermissions._write);
        assertEquals(4, dbank._bankLevel);
        assertEquals(2, (int) dbank._initialBaseRegister);
        assertFalse(dbank._isExtendedMode);
        assertEquals(0, dbank._content.getArraySize());
        assertEquals(02000, dbank._startingAddress);
    }

    @Test
    public void test_simpleExtendedMode() {
        //  simple extended mode program
        String[] source = {
            "          $EXTEND",
            "          $INFO 10 1",
            "",
            "$(0)",
            "DATA      + 077,077",
            "          $RES 16",
            "DATA2     + 0777777,0",
            "",
            "$(1),START$*",
            "          LA        A1,DATA,,B2",
            "          LA        A2,DATA2,,B2",
        };

        Assembler asm = new Assembler();
        RelocatableModule rel = asm.assemble("TESTREL", source, new Assembler.Option[0]);

        Linker.LCPoolSpecification[] ibankPoolSpecs = {
            new Linker.LCPoolSpecification(rel, 1),
        };
        Linker.LCPoolSpecification[] dbankPoolSpecs = {
            new Linker.LCPoolSpecification(rel, 0),
        };

        Linker.BankDeclaration[] bds = {
            new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
                                                .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                                .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                                .setInitialBaseRegister(0)
                                                .setBankLevel(0)
                                                .setBankDescriptorIndex(04)
                                                .setBankName("I1")
                                                .setPoolSpecifications(ibankPoolSpecs)
                                                .setStartingAddress(01000)
                                                .build(),

            new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
                                                .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                                .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                                .setInitialBaseRegister(2)
                                                .setBankLevel(0)
                                                .setBankDescriptorIndex(05)
                                                .setBankName("D1")
                                                .setPoolSpecifications(dbankPoolSpecs)
                                                .setStartingAddress(01000)
                                                .build(),
        };

        Linker.Option[] options = {
            Linker.Option.OPTION_EMIT_SUMMARY,
            Linker.Option.OPTION_EMIT_DICTIONARY,
            Linker.Option.OPTION_EMIT_GENERATED_CODE,
        };

        Linker linker = new Linker();
        AbsoluteModule abs = linker.link("TEST_ASM", bds, options);

        assertNotNull(abs);
        assertEquals(2, abs._loadableBanks.size());

        LoadableBank ibank = abs._loadableBanks.get(04);
        assertTrue(ibank._isExtendedMode);
        assertEquals(2, ibank._content.getArraySize());
        assertEquals(0_100020_021000L, ibank._content.getValue(0));
        assertEquals(0_100040_021021L, ibank._content.getValue(1));

        LoadableBank dbank = abs._loadableBanks.get(05);
        assertFalse(dbank._isExtendedMode);
        assertEquals(18, dbank._content.getArraySize());
        assertEquals(0_000077_000077L, dbank._content.getValue(0));
        assertEquals(0_777777_000000L, dbank._content.getValue(17));
    }

    @Test
    public void test_simpleBasicMode() {
        //  simple basic mode program
        String[] source = {
            "          $BASIC",
            "",
            "$(0)",
            "DATA      + 077,077",
            "          $RES 16",
            "DATA2     + 0777777,0",
            "",
            "$(1),START$*",
            "          LA        A1,DATA,,B2",
            "          LA        A1,DATA2,,B2",
        };

        Assembler asm = new Assembler();
        RelocatableModule rel = asm.assemble("TESTREL", source, new Assembler.Option[0]);

        Linker.LCPoolSpecification[] ibankPoolSpecs = {
            new Linker.LCPoolSpecification(rel, 1),
        };
        Linker.LCPoolSpecification[] dbankPoolSpecs = {
            new Linker.LCPoolSpecification(rel, 0),
        };

        Linker.BankDeclaration[] bds = {
            new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
                                                .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                                .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                                .setInitialBaseRegister(12)
                                                .setBankLevel(0)
                                                .setBankDescriptorIndex(04)
                                                .setBankName("I1")
                                                .setPoolSpecifications(ibankPoolSpecs)
                                                .setStartingAddress(022000)
                .build(),

            new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
                                                .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                                .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                                .setInitialBaseRegister(13)
                                                .setBankLevel(0)
                                                .setBankDescriptorIndex(05)
                                                .setBankName("D1")
                                                .setPoolSpecifications(dbankPoolSpecs)
                                                .setStartingAddress(040000)
                .build(),
        };

        Linker.Option[] options = {
            Linker.Option.OPTION_EMIT_SUMMARY,
            Linker.Option.OPTION_EMIT_DICTIONARY,
            Linker.Option.OPTION_EMIT_GENERATED_CODE,
        };

        Linker linker = new Linker();
        AbsoluteModule abs = linker.link("TEST_ASM", bds, options);

        assertNotNull(abs);
        assertEquals(2, abs._loadableBanks.size());

        LoadableBank ibank = abs._loadableBanks.get(04);
        assertFalse(ibank._isExtendedMode);
        assertEquals(2, ibank._content.getArraySize());
        assertEquals(0_100020_040000L, ibank._content.getValue(0));
        assertEquals(0_100020_040021L, ibank._content.getValue(1));

        LoadableBank dbank = abs._loadableBanks.get(05);
        assertFalse(dbank._isExtendedMode);
        assertEquals(18, dbank._content.getArraySize());
        assertEquals(0_000077_000077L, dbank._content.getValue(0));
        assertEquals(0_777777_000000L, dbank._content.getValue(17));
    }

    @Test
    public void test_multipleRelocatablesBasicMode() {
        //  simple basic mode program
        String[] source1 = {
            "          $BASIC",
            "",
            "$(1),START$*",
            "          LMJ       X11,SUB1",
            "          LMJ       X11,SUB2",
            "          HALT      0",
        };

        String[] source2 = {
            "          $BASIC",
            "",
            "$(1),SUB1*",
            "          J         0,X11",
        };

        String[] source3 = {
            "          $BASIC",
            "",
            "$(1),SUB2*",
            "          J         0,X11",
        };

        Assembler asm = new Assembler();
        RelocatableModule rel1 = asm.assemble("TESTREL1", source1, new Assembler.Option[0]);
        RelocatableModule rel2 = asm.assemble("TESTREL2", source2, new Assembler.Option[0]);
        RelocatableModule rel3 = asm.assemble("TESTREL3", source3, new Assembler.Option[0]);

        Linker.LCPoolSpecification[] ibankPoolSpecs = {
            new Linker.LCPoolSpecification(rel3, 1),
            new Linker.LCPoolSpecification(rel2, 1),
            new Linker.LCPoolSpecification(rel1, 1),
        };
        Linker.LCPoolSpecification[] dbankPoolSpecs = {
        };

        Linker.BankDeclaration[] bds = {
            new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
                                                .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                                .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                                .setInitialBaseRegister(12)
                                                .setBankLevel(0)
                                                .setBankDescriptorIndex(04)
                                                .setBankName("I1")
                                                .setPoolSpecifications(ibankPoolSpecs)
                                                .setStartingAddress(022000)
                .build(),

            new Linker.BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
                                                .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                                .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                                .setInitialBaseRegister(13)
                                                .setBankLevel(0)
                                                .setBankDescriptorIndex(05)
                                                .setBankName("D1")
                                                .setPoolSpecifications(dbankPoolSpecs)
                                                .setStartingAddress(040000)
                .build(),
        };

        Linker.Option[] options = {
            Linker.Option.OPTION_EMIT_SUMMARY,
            Linker.Option.OPTION_EMIT_DICTIONARY,
            Linker.Option.OPTION_EMIT_GENERATED_CODE,
        };

        Linker linker = new Linker();
        AbsoluteModule abs = linker.link("TEST_ASM", bds, options);

        assertNotNull(abs);
        assertEquals(2, abs._loadableBanks.size());

        LoadableBank ibank = abs._loadableBanks.get(04);
        assertFalse(ibank._isExtendedMode);
        assertEquals(5, ibank._content.getArraySize());
        assertEquals(0_742013_000000L, ibank._content.getValue(0));
        assertEquals(0_742013_000000L, ibank._content.getValue(1));
        assertEquals(0_745660_022001L, ibank._content.getValue(2));
        assertEquals(0_745660_022000L, ibank._content.getValue(3));
        assertEquals(0_777760_000000L, ibank._content.getValue(4));

        LoadableBank dbank = abs._loadableBanks.get(05);
        assertFalse(dbank._isExtendedMode);
        assertEquals(0, dbank._content.getArraySize());
    }
}
