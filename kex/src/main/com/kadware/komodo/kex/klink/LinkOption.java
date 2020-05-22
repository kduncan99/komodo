/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

public enum LinkOption {
    ARITHMETIC_FAULT_COMPATIBILITY_MODE,    //  Force AFCM set
    ARITHMETIC_FAULT_NON_INTERRUPT_MODE,    //  Force AFCM clear
    ARITHMETIC_FAULT_INSENSITIVE,           //  Maybe set AFCM clear (see code)
    EMIT_SUMMARY,
    EMIT_DICTIONARY,
    EMIT_GENERATED_CODE,
    EXEC_MODE,
    NO_ENTRY_POINT,
    QUARTER_WORD_MODE,
    SILENT,
    THIRD_WORD_MODE,
}
