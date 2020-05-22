/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

public enum BankType {
    EXTENDED_MODE(0),
    BASIC_MODE(1),
    GATE_BANK(2),
    INDIRECT(3),
    QUEUE_BANK(4),
    QUEUE_BANK_REPOSITORY(6);

    final int _code;

    BankType(
        final int code
    ) {
        _code = code;
    }
}
