/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kexec.include;

/**
 * Source code describing a bank descriptor
 */
public class BankDescriptor {

    public static final String[] SOURCE = {
        ". .....................................",
        ". Bank descriptor equfs",
        ".",
        "BD_GAPSAP    $EQUF      0,,S1",
        "BD_TYPE      $EQUF      0,,S2",
        "BD_ACCLOCK   $EQUF      0,,H2",
        "BD_LOWER     $EQUF      1,,Q1",
        "BD_UPPER     $EQUF      1,,W     . Actually, only bits 9-35",
        "BD_ADDR      $EQUF      2        . Abs Address 2 words",
        ". ....................................."
    };
}
