/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

public enum FacStatusCategory {
    Info("I"),
    Warning("W"),
    Error("E");

    private final String _token;

    FacStatusCategory(final String ch) {
        _token = ch;
    }

    @Override
    public String toString() {
        return _token;
    }
}
