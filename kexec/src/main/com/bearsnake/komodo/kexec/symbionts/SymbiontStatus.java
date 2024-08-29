/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

public enum SymbiontStatus {
    Active("ACTIVE"),
    Inactive("INACTIVE"),
    Locked("LOCKED"),
    Suspended("SUSPENDED"),
    Waiting("WAITING");

    private final String _string;

    SymbiontStatus(String string) {
        _string = string;
    }

    @Override
    public String toString() { return _string; }
}
