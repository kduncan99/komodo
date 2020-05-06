/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.binaries.sandbox;

public class IntHandlers {

    public static final String[] SOURCE = {
        ". INTHANDLERS",
        ". Copyright (C) 2019-2020 by Kurt Duncan - All Rights Reserved",
        ".",
        ". This code contains the interrupt handlers for some simple thing running in EXEC mode",
        ".",
        "$(0)      $LIT",
        "DESREG    + 000001,0 . Designator Register setting for IAR",
        ".",
        "$(1)      $LIT",
        "IH$NOTIMPL* .",
        "          LD        DESREG,,B0",
        "          LR,U      R10,0400 . Undefined/Unimplemented interrupt",
        "          IAR       0,,B0",
    };
}
