/*
 * Copyright (c) 2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.binaries.sandbox;

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
        "          SYSFORM1  031,0,0,14,0       . subfunc, status, flags, msglen, reserved",
        "          +         0                  . console identifier (zero)",
        "          +         BDIREF$+MSG,MSG    . L,BDI,Offset of text",
        "                                       . We should do MSG-FIRST$, but that doesn't work right yet",
        "",
        "$(1)      .",
        "START$*",
        "          SYSC      SYSCPKT,,B2",
        "          HALT      0",
    };
}
