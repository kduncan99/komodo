/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

public enum NodeStatus {
    Down("DN"),
    Reserved("RV"),
    Suspended("SU"),
    Up("UP");

    private final String _displayString;

    NodeStatus(final String displayString) {
        _displayString = displayString;
    }
}
