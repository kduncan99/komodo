/*
 * Copyright (c) 2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.binaries.sandbox;

import com.kadware.komodo.minalib.Assembler;
import com.kadware.komodo.minalib.RelocatableModule;

public class Sandbox {

    public static final String[] SOURCE = {
        ". TEST",
        ". Copyright (C) 2020 by Kurt Duncan - All Rights Reserved",
        ".",
        ". A testing sandbox.",
        ".",
        "          $ASCII",
        "          $EXTEND",
        "",
        "$(0)      .",
        "MSG       'This is a test'",
        "",
        "SYSFORM1  $FORM     6,6,6,9,9",
        "SYSCPKT   .",
        "          SYSFORM1  031,0,0,14,0        . subfunc, status, flags, msglen, reserved",
        "          +         0                   . console identifier (zero)",
        "          +         032,MSG             . L,BDI,Offset of text",
        "",
        "$(1)      .",
        "START*",
        "          SYSC      SYSCPKT",
        "          HALT      0",
    };

    public static void main(
        final String[] args
    ) {
        Assembler.Option[] optionSet = {
            Assembler.Option.EMIT_MODULE_SUMMARY,
            Assembler.Option.EMIT_SOURCE,
            Assembler.Option.EMIT_GENERATED_CODE,
            Assembler.Option.EMIT_DICTIONARY,
            };

        Assembler asm = new Assembler();
        RelocatableModule module = asm.assemble("TEST", SOURCE, optionSet);
    }
}
