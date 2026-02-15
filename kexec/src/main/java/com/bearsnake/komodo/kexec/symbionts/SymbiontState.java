/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

/**
 * Reflects internal symbiont-specific state of a particular symbiont device
 */
public enum SymbiontState {
    IoError("IO ERROR"),
    Reading("READING"),
    Printing("PRINTING"),
    RePrinting("REPRINTING"),
    Punching("PUNCHING"),
    RePunching("REPUNCHING"),
    Skipping("SKIPPING"),
    Stopped("STOPPED");

    private final String _string;

    SymbiontState(String string) { _string = string; }

    @Override public String toString() { return _string; }
}
