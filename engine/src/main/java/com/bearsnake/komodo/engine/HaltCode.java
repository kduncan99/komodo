/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

public enum HaltCode {
    NONE,
    CANNOT_LOAD_INTERRUPT_HANDLER_BANK,
    HARDWARE_CHECK_DURING_FAULT_HANDLING,
    HLTJ_INSTRUCTION,
    ICS_OVERFLOW,
    INTERRUPT_DURING_RESET,
    INVALID_INTERRUPT_HANDLER_BANK_TYPE,
    INVALID_INTERRUPT_VECTOR,
    UNIT_TEST_STOP,
}
