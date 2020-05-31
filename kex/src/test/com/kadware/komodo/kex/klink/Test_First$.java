/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

import com.kadware.komodo.kex.RelocatableModule;
import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.AssemblerOption;
import com.kadware.komodo.kex.kasm.AssemblerResult;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_First$ {

    @Test
    public void simple() {

        final String[] source = {
            "$(0) .",
            " + 01000+FIRST$",
            " + FIRST$+02000",
            " + 03000-FIRST$",
            " + FIRST$-01",
        };

        AssemblerOption[] asmOpts = {
            AssemblerOption.EMIT_DICTIONARY,
            AssemblerOption.EMIT_MODULE_SUMMARY,
            AssemblerOption.EMIT_GENERATED_CODE,
            AssemblerOption.EMIT_DICTIONARY
        };
        Assembler asm = new Assembler.Builder().setSource(source)
                                               .setModuleName("TEST")
                                               .setOptions(asmOpts)
                                               .build();
        AssemblerResult asmResult = asm.assemble();

        LinkOption[] linkOpts = {
            LinkOption.EMIT_GENERATED_CODE,
            LinkOption.EMIT_SUMMARY,
            LinkOption.NO_ENTRY_POINT,
        };

        BankDeclaration.BankDeclarationOption[] bankOpts = {
            BankDeclaration.BankDeclarationOption.DBANK,
            BankDeclaration.BankDeclarationOption.EXTENDED_MODE
        };

        LCPoolSpecification[] poolSpecs = { new LCPoolSpecification(asmResult._relocatableModule, 0) };

        BankDeclaration[] bankDecls = {
            new BankDeclaration.Builder().setPoolSpecifications(poolSpecs)
                                         .setStartingAddress(01000)
                                         .setBankName("TESTBANK").setOptions(bankOpts)
                                         .build()
        };

        Linker linker = new Linker.Builder().setBankDeclarations(bankDecls)
                                            .setOptions(linkOpts)
                                            .setModuleName("TEST")
                                            .build();
        LinkResult linkResult = linker.link(LinkType.MULTI_BANKED_BINARY);

        assertEquals(0, linkResult._errorCount);
        assertNotNull(linkResult._bankDescriptors);
        assertEquals(1, linkResult._bankDescriptors.length);
        BankDescriptor bankDesc = linkResult._bankDescriptors[0];
        assertEquals(4, bankDesc._content.length);
        assertEquals(02000L, bankDesc._content[0]);
        assertEquals(03000L, bankDesc._content[1]);
        assertEquals(02000L, bankDesc._content[2]);
        assertEquals(0777L, bankDesc._content[3]);
    }
}
