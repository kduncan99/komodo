/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.binaries.sandbox;

public class ICS {

    public static final String[] SOURCE = {
        ". ICS",
        ". Copyright (C) 2019-2020 by Kurt Duncan - All Rights Reserved",
        ".",
        ". Space for the interrupt control stack",
        ".",
        "$(0)",
        "ICS$SIZE   $EQU 64             . Big enough for 4 16-word frames",
        "ICS$STACK* $RES ICS$SIZE",
        ".",
    };
}
