/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;
import com.kadware.komodo.baselib.BankType;
import com.kadware.komodo.kex.RelocatableModule;
import com.kadware.komodo.kex.klink.BankDeclaration;
import com.kadware.komodo.kex.klink.LoadableBank;
import com.kadware.komodo.kex.klink.LCPoolSpecification;
import com.kadware.komodo.kex.klink.LinkOption;
import com.kadware.komodo.kex.klink.LinkResult;
import com.kadware.komodo.kex.klink.LinkType;
import com.kadware.komodo.kex.klink.Linker;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

@SuppressWarnings("PointlessArithmeticExpression")
public class Test_Linker {

    private static final Set<LinkOption> BASE_OPTIONS = new HashSet<>();
    static {
        BASE_OPTIONS.add(LinkOption.EMIT_SUMMARY);
        BASE_OPTIONS.add(LinkOption.EMIT_DICTIONARY);
        BASE_OPTIONS.add(LinkOption.EMIT_GENERATED_CODE);
    };

    //  absolute module tests
    //  TODO


    //  binary mode tests

    /**
     * No input code provided - one bank is created, with no content
     */
    @Test
    public void binary_empty() {
        HashSet<LinkOption> options = new HashSet<>(BASE_OPTIONS);
        options.add(LinkOption.NO_ENTRY_POINT);

        Linker linker = new Linker.Builder().setModuleName("TEST")
                                            .setOptions(options)
                                            .build();
        LinkResult result = linker.link(LinkType.BINARY);

        assertEquals("TEST", result._moduleName);
        assertEquals(0, result._errorCount);
        assertNull(result._absoluteModule);
        assertNull(result._objectModule);
        assertNotNull(result._loadableBanks);
        assertEquals(1, result._loadableBanks.length);

        AccessInfo accInfo = new AccessInfo(0, 0);
        AccessPermissions gap = new AccessPermissions(false, false, false);
        AccessPermissions sap = new AccessPermissions(true, true, true);

        LoadableBank bd040 = result._loadableBanks[0];
        assertEquals("TEST", bd040._bankName);
        assertEquals(0, bd040._bankLevel);
        assertEquals(040, bd040._bankDescriptorIndex);
        assertEquals(0, bd040._lowerLimit);
        assertEquals(0_777777, bd040._upperLimit);
        assertEquals(BankType.BasicMode, bd040._bankType);
        assertEquals(accInfo, bd040._accessInfo);
        assertEquals(gap, bd040._generalPermissions);
        assertEquals(sap, bd040._specialPermissions);
        assertEquals(0, bd040._content.length);
    }

    /**
     * Single relocatable module, extended mode required
     */
    @Test
    public void binary_simple() {
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
            "          LA        A1,DATA,,B0",
            "          LA        A2,DATA2,,B0",
            "          $END START$",
        };

        AssemblerOption[] asmOpts = { AssemblerOption.EMIT_SOURCE,
                                      AssemblerOption.EMIT_GENERATED_CODE,
                                      AssemblerOption.EMIT_DICTIONARY,
                                      AssemblerOption.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler.Builder().setModuleName("TESTREL")
                                               .setSource(source)
                                               .setOptions(asmOpts)
                                               .build();
        AssemblerResult asmResult = asm.assemble();
        assertFalse(asmResult._diagnostics.hasError());

        RelocatableModule[] relModules = {asmResult._relocatableModule};
        Linker linker = new Linker.Builder().setModuleName("TEST")
                                            .setRelocatableModules(relModules)
                                            .setOptions(BASE_OPTIONS)
                                            .build();
        LinkResult linkResult = linker.link(LinkType.BINARY);
        assertNotNull(linkResult);
        assertEquals(0, linkResult._errorCount);
        assertNotNull(linkResult._loadableBanks);
        assertEquals(1, linkResult._loadableBanks.length);

        LoadableBank bd = linkResult._loadableBanks[0];

        assertEquals(BankType.ExtendedMode, bd._bankType);
        assertEquals(20, bd._content.length);
        int lc0Offset = 0;
        int lc1Offset = 18;

        assertEquals(0_000077_000077L, bd._content[lc0Offset + 0]);
        assertEquals(0_777777_000000L, bd._content[lc0Offset + 17]);
        assertEquals(0_100020_000000L, bd._content[lc1Offset + 0]);
        assertEquals(0_100040_000021L, bd._content[lc1Offset + 1]);
    }

    //  TODO - need more tests


    //  multi-bank binary tests

    /**
     * No input code provided - should produce an error, as binarymb requires at least one bank declaration
     */
    @Test
    public void binarymb_empty() {
        LinkOption[] options = {
            LinkOption.NO_ENTRY_POINT,
            LinkOption.EMIT_SUMMARY,
            LinkOption.EMIT_DICTIONARY
        };

        Linker linker = new Linker.Builder().setModuleName("TEST")
                                            .setOptions(options)
                                            .build();
        LinkResult result = linker.link(LinkType.MULTI_BANKED_BINARY);

        assertEquals("TEST", result._moduleName);
        assertEquals(1, result._errorCount);
        assertNull(result._absoluteModule);
        assertNull(result._objectModule);
        assertNull(result._loadableBanks);
    }

    /**
     * Single relocatable module, extended mode required
     */
    @Test
    public void binarymb_simple() {
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
            "          LA        A1,DATA,,B0",
            "          LA        A2,DATA2,,B0",
            "          $END START$",
            };

        AssemblerOption[] asmOpts = { AssemblerOption.EMIT_SOURCE,
                                      AssemblerOption.EMIT_GENERATED_CODE,
                                      AssemblerOption.EMIT_DICTIONARY,
                                      AssemblerOption.EMIT_MODULE_SUMMARY };
        Assembler asm = new Assembler.Builder().setModuleName("TESTREL")
                                               .setSource(source)
                                               .setOptions(asmOpts)
                                               .build();
        AssemblerResult asmResult = asm.assemble();
        assertFalse(asmResult._diagnostics.hasError());

        Set<BankDeclaration.BankDeclarationOption> bdOptions = new HashSet<>();
        bdOptions.add(BankDeclaration.BankDeclarationOption.DYNAMIC);
        bdOptions.add(BankDeclaration.BankDeclarationOption.EXTENDED_MODE);

        AccessInfo accessInfo = new AccessInfo(1, 1);
        AccessPermissions gap = new AccessPermissions(false, true, false);
        AccessPermissions sap = new AccessPermissions(false, true, true);

        LCPoolSpecification lcPoolSpecs[] = {
            new LCPoolSpecification(asmResult._relocatableModule, 0),
            new LCPoolSpecification(asmResult._relocatableModule, 1),
        };

        BankDeclaration[] bankDeclarations = {
            new BankDeclaration.Builder().setGeneralAccessPermissions(gap)
                                         .setSpecialAccessPermissions(sap)
                                         .setBankLevel(1)
                                         .setBankDescriptorIndex(0100)
                                         .setBankName("TESTBANK")
                                         .setOptions(bdOptions)
                                         .setPoolSpecifications(lcPoolSpecs)
                                         .setAccessInfo(accessInfo)
                                         .setStartingAddress(01000)
                                         .build()
        };

        Linker linker = new Linker.Builder().setModuleName("TEST")
                                            .setBankDeclarations(bankDeclarations)
                                            .setOptions(BASE_OPTIONS)
                                            .build();
        LinkResult linkResult = linker.link(LinkType.MULTI_BANKED_BINARY);
        assertNotNull(linkResult);
        assertEquals(0, linkResult._errorCount);
        assertNotNull(linkResult._loadableBanks);
        assertEquals("TEST", linkResult._moduleName);
        assertEquals(1, linkResult._loadableBanks.length);

        LoadableBank bd = linkResult._loadableBanks[0];

        assertEquals(BankType.ExtendedMode, bd._bankType);
        assertEquals(1, bd._bankLevel);
        assertEquals(0100, bd._bankDescriptorIndex);
        assertEquals("TESTBANK", bd._bankName);
        assertEquals(accessInfo, bd._accessInfo);
        assertEquals(01000, bd._lowerLimit);
        assertEquals(01023, bd._upperLimit);
        assertEquals(20, bd._content.length);
        int lc0Offset = 0;
        int lc1Offset = 18;

        assertEquals(0_000077_000077L, bd._content[lc0Offset + 0]);
        assertEquals(0_777777_000000L, bd._content[lc0Offset + 17]);
        assertEquals(0_100020_001000L, bd._content[lc1Offset + 0]);
        assertEquals(0_100040_001021L, bd._content[lc1Offset + 1]);
    }

    //  TODO - need more tests


    //  object module tests
    //  TODO

    //  TODO afcm sensitive tests
    //  TODO partial-word mode sensitive tests


//    @Test
//    public void test_simpleBasicMode() {
//        //  simple basic mode program
//        String[] source = {
//            "          $BASIC",
//            "",
//            "$(0)",
//            "DATA      + 077,077",
//            "          $RES 16",
//            "DATA2     + 0777777,0",
//            "",
//            "$(1),START$*",
//            "          LA        A1,DATA,,B2",
//            "          LA        A1,DATA2,,B2",
//        };
//
//        Assembler.AssemblerResult result = Assembler.assemble("TESTREL", source);
//
//        LCPoolSpecification[] ibankPoolSpecs = {
//            new LCPoolSpecification(result._relocatableModule, 1),
//        };
//        LCPoolSpecification[] dbankPoolSpecs = {
//            new LCPoolSpecification(result._relocatableModule, 0),
//        };
//
//        BankDeclaration[] bds = {
//            new BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
//                                         .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
//                                         .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
//                                         .setInitialBaseRegister(12)
//                                         .setBankLevel(0)
//                                         .setBankDescriptorIndex(04)
//                                         .setBankName("I1")
//                                         .setPoolSpecifications(ibankPoolSpecs)
//                                         .setStartingAddress(022000)
//                .build(),
//
//            new BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
//                                         .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
//                                         .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
//                                         .setInitialBaseRegister(13)
//                                         .setBankLevel(0)
//                                         .setBankDescriptorIndex(05)
//                                         .setBankName("D1")
//                                         .setPoolSpecifications(dbankPoolSpecs)
//                                         .setStartingAddress(040000)
//                .build(),
//        };
//
//        LinkOption[] options = {
//            LinkOption.EMIT_SUMMARY,
//            LinkOption.EMIT_DICTIONARY,
//            LinkOption.EMIT_GENERATED_CODE,
//            };
//
//        Linker linker = new Linker();
//        OldAbsoluteModule abs = linker.link("TEST_ASM", bds, 0, options);
//
//        assertNotNull(abs);
//        assertEquals(2, abs._loadableBanks.size());
//
//        LoadableBank ibank = abs._loadableBanks.get(04);
//        assertFalse(ibank._isExtendedMode);
//        assertEquals(2, ibank._content.getSize());
//        assertEquals(0_100020_040000L, ibank._content.get(0));
//        assertEquals(0_100020_040021L, ibank._content.get(1));
//
//        LoadableBank dbank = abs._loadableBanks.get(05);
//        assertFalse(dbank._isExtendedMode);
//        assertEquals(18, dbank._content.getSize());
//        assertEquals(0_000077_000077L, dbank._content.get(0));
//        assertEquals(0_777777_000000L, dbank._content.get(17));
//    }

//    @Test
//    public void test_multipleRelocatablesBasicMode() {
//        //  simple basic mode program
//        String[] source1 = {
//            "          $BASIC",
//            "",
//            "$(1),START$*",
//            "          LMJ       X11,SUB1",
//            "          LMJ       X11,SUB2",
//            "          HALT      0",
//        };
//
//        String[] source2 = {
//            "          $BASIC",
//            "",
//            "$(1),SUB1*",
//            "          J         0,X11",
//        };
//
//        String[] source3 = {
//            "          $BASIC",
//            "",
//            "$(1),SUB2*",
//            "          J         0,X11",
//        };
//
//        Assembler.AssemblerResult result1 = Assembler.assemble("TESTREL1", source1);
//        Assembler.AssemblerResult result2 = Assembler.assemble("TESTREL2", source2);
//        Assembler.AssemblerResult result3 = Assembler.assemble("TESTREL3", source3);
//
//        LCPoolSpecification[] ibankPoolSpecs = {
//            new LCPoolSpecification(result3._relocatableModule, 1),
//            new LCPoolSpecification(result2._relocatableModule, 1),
//            new LCPoolSpecification(result1._relocatableModule, 1),
//        };
//        LCPoolSpecification[] dbankPoolSpecs = { };
//
//        BankDeclaration[] bds = {
//            new BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
//                                         .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
//                                         .setSpecialAccessPermissions(new AccessPermissions(true, true, true))
//                                         .setInitialBaseRegister(12)
//                                         .setBankLevel(0)
//                                         .setBankDescriptorIndex(04)
//                                         .setBankName("I1")
//                                         .setPoolSpecifications(ibankPoolSpecs)
//                                         .setStartingAddress(022000)
//                .build(),
//
//            new BankDeclaration.Builder().setAccessInfo(new AccessInfo((byte)0, (short)0))
//                                         .setGeneralAccessPermissions(new AccessPermissions(false, false, false))
//                                         .setSpecialAccessPermissions(new AccessPermissions(false, true, true))
//                                         .setInitialBaseRegister(13)
//                                         .setBankLevel(0)
//                                         .setBankDescriptorIndex(05)
//                                         .setBankName("D1")
//                                         .setPoolSpecifications(dbankPoolSpecs)
//                                         .setStartingAddress(040000)
//                .build(),
//        };
//
//        LinkOption[] options = {
//            LinkOption.EMIT_SUMMARY,
//            LinkOption.EMIT_DICTIONARY,
//            LinkOption.EMIT_GENERATED_CODE,
//            };
//
//        Linker linker = new Linker();
//        OldAbsoluteModule abs = linker.link("TEST_ASM", bds, 0, options);
//
//        assertNotNull(abs);
//        assertEquals(2, abs._loadableBanks.size());
//
//        LoadableBank ibank = abs._loadableBanks.get(04);
//        assertFalse(ibank._isExtendedMode);
//        assertEquals(5, ibank._content.getSize());
//        assertEquals(0_742013_000000L, ibank._content.get(0));
//        assertEquals(0_742013_000000L, ibank._content.get(1));
//        assertEquals(0_745660_022001L, ibank._content.get(2));
//        assertEquals(0_745660_022000L, ibank._content.get(3));
//        assertEquals(0_777760_000000L, ibank._content.get(4));
//
//        LoadableBank dbank = abs._loadableBanks.get(05);
//        assertFalse(dbank._isExtendedMode);
//        assertEquals(0, dbank._content.getSize());
//    }
}
