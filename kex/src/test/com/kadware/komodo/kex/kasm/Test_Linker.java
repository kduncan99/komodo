/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;
import com.kadware.komodo.kex.klink.BankDeclaration;
import com.kadware.komodo.kex.klink.LCPoolSpecification;
import com.kadware.komodo.kex.klink.LinkOption;
import com.kadware.komodo.kex.klink.Linker;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_Linker {

    @Test
    public void test_empty() {

        BankDeclaration[] bds = {
            new BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 0, (short) 010))
                                         .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                         .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                         .setInitialBaseRegister(0)
                                         .setBankLevel(02)
                                         .setBankDescriptorIndex(04)
                                         .setBankName("I1")
                                         .setPoolSpecifications(new LCPoolSpecification[0])
                                         .setStartingAddress(01000)
                                                .build(),
            new BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte) 3, (short) 077))
                                         .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                         .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
                                         .setInitialBaseRegister(2)
                                         .setBankLevel(04)
                                         .setBankDescriptorIndex(05)
                                         .setBankName("D1")
                                         .setPoolSpecifications(new LCPoolSpecification[0])
                                         .setStartingAddress(02000)
                                                .build(),
        };

        LinkOption[] options = {
            LinkOption.NO_ENTRY_POINT,
            LinkOption.EMIT_SUMMARY,
            LinkOption.EMIT_DICTIONARY
        };

        Linker linker = new Linker();
        OldAbsoluteModule abs = linker.link("TEST_ASM", bds, 0, options);

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
        assertEquals(0, ibank._content.getSize());
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
        assertEquals(0, dbank._content.getSize());
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

        Assembler.Result result = Assembler.assemble("TESTREL", source);

        LCPoolSpecification[] ibankPoolSpecs = {
            new LCPoolSpecification(result._relocatableModule, 1),
        };
        LCPoolSpecification[] dbankPoolSpecs = {
            new LCPoolSpecification(result._relocatableModule, 0),
        };

        BankDeclaration[] bds = {
            new BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
                                         .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                         .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                         .setInitialBaseRegister(0)
                                         .setBankLevel(0)
                                         .setBankDescriptorIndex(04)
                                         .setBankName("I1")
                                         .setPoolSpecifications(ibankPoolSpecs)
                                         .setStartingAddress(01000)
                                                .build(),

            new BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
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

        LinkOption[] options = {
            LinkOption.EMIT_SUMMARY,
            LinkOption.EMIT_DICTIONARY,
            LinkOption.EMIT_GENERATED_CODE,
            };

        Linker linker = new Linker();
        OldAbsoluteModule abs = linker.link("TEST_ASM", bds, 32, options);

        assertNotNull(abs);
        assertEquals(3, abs._loadableBanks.size());

        LoadableBank ibank = abs._loadableBanks.get(04);
        assertTrue(ibank._isExtendedMode);
        assertEquals(2, ibank._content.getSize());
        assertEquals(0_100020_021000L, ibank._content.get(0));
        assertEquals(0_100040_021021L, ibank._content.get(1));

        LoadableBank dbank = abs._loadableBanks.get(05);
        assertFalse(dbank._isExtendedMode);
        assertEquals(18, dbank._content.getSize());
        assertEquals(0_000077_000077L, dbank._content.get(0));
        assertEquals(0_777777_000000L, dbank._content.get(17));

        LoadableBank rcsbank = abs._loadableBanks.get(06);
        assertTrue(rcsbank._isExtendedMode);
        assertEquals(256, rcsbank._content.getSize());
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

        Assembler.Result result = Assembler.assemble("TESTREL", source);

        LCPoolSpecification[] ibankPoolSpecs = {
            new LCPoolSpecification(result._relocatableModule, 1),
        };
        LCPoolSpecification[] dbankPoolSpecs = {
            new LCPoolSpecification(result._relocatableModule, 0),
        };

        BankDeclaration[] bds = {
            new BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
                                         .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                         .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                         .setInitialBaseRegister(12)
                                         .setBankLevel(0)
                                         .setBankDescriptorIndex(04)
                                         .setBankName("I1")
                                         .setPoolSpecifications(ibankPoolSpecs)
                                         .setStartingAddress(022000)
                .build(),

            new BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
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

        LinkOption[] options = {
            LinkOption.EMIT_SUMMARY,
            LinkOption.EMIT_DICTIONARY,
            LinkOption.EMIT_GENERATED_CODE,
            };

        Linker linker = new Linker();
        OldAbsoluteModule abs = linker.link("TEST_ASM", bds, 0, options);

        assertNotNull(abs);
        assertEquals(2, abs._loadableBanks.size());

        LoadableBank ibank = abs._loadableBanks.get(04);
        assertFalse(ibank._isExtendedMode);
        assertEquals(2, ibank._content.getSize());
        assertEquals(0_100020_040000L, ibank._content.get(0));
        assertEquals(0_100020_040021L, ibank._content.get(1));

        LoadableBank dbank = abs._loadableBanks.get(05);
        assertFalse(dbank._isExtendedMode);
        assertEquals(18, dbank._content.getSize());
        assertEquals(0_000077_000077L, dbank._content.get(0));
        assertEquals(0_777777_000000L, dbank._content.get(17));
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

        Assembler.Result result1 = Assembler.assemble("TESTREL1", source1);
        Assembler.Result result2 = Assembler.assemble("TESTREL2", source2);
        Assembler.Result result3 = Assembler.assemble("TESTREL3", source3);

        LCPoolSpecification[] ibankPoolSpecs = {
            new LCPoolSpecification(result3._relocatableModule, 1),
            new LCPoolSpecification(result2._relocatableModule, 1),
            new LCPoolSpecification(result1._relocatableModule, 1),
        };
        LCPoolSpecification[] dbankPoolSpecs = { };

        BankDeclaration[] bds = {
            new BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
                                         .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
                                         .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
                                         .setInitialBaseRegister(12)
                                         .setBankLevel(0)
                                         .setBankDescriptorIndex(04)
                                         .setBankName("I1")
                                         .setPoolSpecifications(ibankPoolSpecs)
                                         .setStartingAddress(022000)
                .build(),

            new BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
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

        LinkOption[] options = {
            LinkOption.EMIT_SUMMARY,
            LinkOption.EMIT_DICTIONARY,
            LinkOption.EMIT_GENERATED_CODE,
            };

        Linker linker = new Linker();
        OldAbsoluteModule abs = linker.link("TEST_ASM", bds, 0, options);

        assertNotNull(abs);
        assertEquals(2, abs._loadableBanks.size());

        LoadableBank ibank = abs._loadableBanks.get(04);
        assertFalse(ibank._isExtendedMode);
        assertEquals(5, ibank._content.getSize());
        assertEquals(0_742013_000000L, ibank._content.get(0));
        assertEquals(0_742013_000000L, ibank._content.get(1));
        assertEquals(0_745660_022001L, ibank._content.get(2));
        assertEquals(0_745660_022000L, ibank._content.get(3));
        assertEquals(0_777760_000000L, ibank._content.get(4));

        LoadableBank dbank = abs._loadableBanks.get(05);
        assertFalse(dbank._isExtendedMode);
        assertEquals(0, dbank._content.getSize());
    }
}
