/*
 * Copyright (c) 2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.binaries.sandbox;

import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;
import com.kadware.komodo.minalib.AbsoluteModule;
import com.kadware.komodo.minalib.Assembler;
import com.kadware.komodo.minalib.Linker;
import com.kadware.komodo.minalib.RelocatableModule;

public class Builder {

    private static final Assembler.Option[] assemblerOptions = {
        Assembler.Option.EMIT_MODULE_SUMMARY,
        Assembler.Option.EMIT_SOURCE,
        Assembler.Option.EMIT_GENERATED_CODE,
        Assembler.Option.EMIT_DICTIONARY
    };

    private static final Linker.Option[] linkerOptions = {
        Linker.Option.OPTION_EMIT_SUMMARY,
        Linker.Option.OPTION_QUARTER_WORD_MODE,
        Linker.Option.OPTION_EMIT_GENERATED_CODE,
        Linker.Option.OPTION_NO_ENTRY_POINT
    };

    public static void main(
        final String[] args
    ) {
        Assembler asm = new Assembler();
        RelocatableModule level0BDTRel = asm.assemble("Level0BDT", Level0BDT.SOURCE, assemblerOptions);
        RelocatableModule intHandlersRel = asm.assemble("IntHandlers", IntHandlers.SOURCE, assemblerOptions);
        RelocatableModule sandboxRel = asm.assemble("Sandbox", Sandbox.SOURCE, assemblerOptions);

        //  Level 0 BDT - BDI 0,0
        Linker.LCPoolSpecification[] poolSpecs000000 = {
            new Linker.LCPoolSpecification(level0BDTRel, 0)
        };

        Linker.BankDeclaration bankDecl000000 =
            new Linker.BankDeclaration.Builder()
                .setAccessInfo(new AccessInfo((byte) 0, (short) 0))
                .setBankName("LEVEL0BDT")
                .setBankDescriptorIndex(0)
                .setBankLevel(0)
                .setNeedsExtendedMode(true)
                .setStartingAddress(0)
                .setPoolSpecifications(poolSpecs000000)
                .setGeneralAccessPermissions(new AccessPermissions(false, true, false))
                .setSpecialAccessPermissions(new AccessPermissions(false, true, false))
                .build();

        //  Code - BDI 0,32
        Linker.LCPoolSpecification[] poolSpecs000040 = {
            new Linker.LCPoolSpecification(sandboxRel, 1),
            new Linker.LCPoolSpecification(sandboxRel, 0)
        };

        Linker.BankDeclaration bankDecl000040 =
            new Linker.BankDeclaration.Builder()
                .setAccessInfo(new AccessInfo((byte) 0, (short) 0))
                .setBankName("CODE")
                .setBankDescriptorIndex(040)
                .setBankLevel(0)
                .setNeedsExtendedMode(true)
                .setStartingAddress(01000)
                .setPoolSpecifications(poolSpecs000040)
                .setGeneralAccessPermissions(new AccessPermissions(true, true, false))
                .setSpecialAccessPermissions(new AccessPermissions(true, true, false))
                .build();

        //  Interrupt handlers - BDI 0,33
        Linker.LCPoolSpecification[] poolSpecs000041 = {
            new Linker.LCPoolSpecification(intHandlersRel, 1),
            new Linker.LCPoolSpecification(intHandlersRel, 0)
        };

        Linker.BankDeclaration bankDecl000041 =
            new Linker.BankDeclaration.Builder()
                .setAccessInfo(new AccessInfo((byte) 0, (short) 0))
                .setBankName("INTHANDLERS")
                .setBankDescriptorIndex(041)
                .setBankLevel(0)
                .setNeedsExtendedMode(true)
                .setStartingAddress(01000)
                .setPoolSpecifications(poolSpecs000041)
                .setGeneralAccessPermissions(new AccessPermissions(true, true, false))
                .setSpecialAccessPermissions(new AccessPermissions(true, true, false))
                .build();

        //  Bring it all together
        Linker.BankDeclaration[] bankDeclarations = {
            bankDecl000000,
            bankDecl000040,
            bankDecl000041
        };

        Linker linker = new Linker();
        AbsoluteModule abs =  linker.link("SANDBOX", bankDeclarations, 0, linkerOptions);
    }
}
