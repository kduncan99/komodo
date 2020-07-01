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
        assertEquals(01000, bd040._lowerLimit);
        assertEquals(0777, bd040._upperLimit);
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
        assertEquals(0_100020_001000L, bd._content[lc1Offset + 0]);
        assertEquals(0_100040_001021L, bd._content[lc1Offset + 1]);
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

        LCPoolSpecification[] lcPoolSpecs = {
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


    @Test
    public void test_multipleRelocatablesBasicMode() {
        //  simple basic mode program
        String[] source1 = {
            "          $EXTEND",
            "          $INFO 10 1",
            "$(1)      $LIT",
            "START",
            "",
            "          $BASIC",
            "$(3)",
            "BMSTART",
            "          LMJ       X11,SUB1",
            "          LMJ       X11,SUB2",
            "          HALT      0"
        };

        String[] source2 = {
            "          $BASIC",
            "",
            "$(1),SUB1*",
            "          J         0,X11"
        };

        String[] source3 = {
            "          $BASIC",
            "",
            "$(1),SUB2*",
            "          J         0,X11"
        };

        AssemblerOption[] asmOpts = {
            AssemblerOption.EMIT_SOURCE,
            AssemblerOption.EMIT_GENERATED_CODE,
            AssemblerOption.EMIT_DICTIONARY,
            AssemblerOption.EMIT_MODULE_SUMMARY
        };

        Assembler asm1 = new Assembler.Builder().setModuleName("TESTREL1").setSource(source1).setOptions(asmOpts).build();
        Assembler asm2 = new Assembler.Builder().setModuleName("TESTREL1").setSource(source2).setOptions(asmOpts).build();
        Assembler asm3 = new Assembler.Builder().setModuleName("TESTREL1").setSource(source3).setOptions(asmOpts).build();

        AssemblerResult asmResult1 = asm1.assemble();
        AssemblerResult asmResult2 = asm2.assemble();
        AssemblerResult asmResult3 = asm3.assemble();

        assertFalse(asmResult1._diagnostics.hasError());
        assertFalse(asmResult2._diagnostics.hasError());
        assertFalse(asmResult3._diagnostics.hasError());

        RelocatableModule[] relModules = {
            asmResult1._relocatableModule,
            asmResult2._relocatableModule,
            asmResult3._relocatableModule
        };

        LinkOption[] linkOpts = {
            LinkOption.EMIT_SUMMARY,
            LinkOption.EMIT_DICTIONARY,
            LinkOption.EMIT_GENERATED_CODE
        };

        Linker linker = new Linker.Builder().setOptions(linkOpts).setRelocatableModules(relModules).build();
        LinkResult linkResult = linker.link(LinkType.BINARY);

        assertNotNull(linkResult._loadableBanks);
        assertEquals(1, linkResult._loadableBanks.length);

        LoadableBank ibank = linkResult._loadableBanks[0];
        assertEquals(ibank._bankType, BankType.ExtendedMode);
        assertEquals(5, ibank._content.length);
    }
}
