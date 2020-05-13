/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.diagnostics;

import com.kadware.komodo.kex.kasm.Locale;

/**
 * Class for reporting truncation diagnostic messages - i.e., generated code won't fit into the field it is intended to
 */
public class TruncationDiagnostic extends Diagnostic {

    public TruncationDiagnostic(
        final Locale locale,
        final String message
    ) {
        super(locale, message);
    }

    public TruncationDiagnostic(
        final Locale locale,
        final int startingBit,
        final int endingBit
    ) {
        super(locale, String.format("Significant data truncated in bits %d to %d", startingBit, endingBit));
    }

    /**
     * Get the level associated with this instance
     */
    @Override
    public Level getLevel(
    ) {
        return Level.Truncation;
    }

    @Override
    public boolean isError() { return true; }
}
