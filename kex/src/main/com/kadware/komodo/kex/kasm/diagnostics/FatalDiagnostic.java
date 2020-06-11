/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.diagnostics;

import com.kadware.komodo.kex.kasm.Locale;

/**
 * Class for fatal diagnostic messages (situations which kill the assembler - if any)
 */
public class FatalDiagnostic extends Diagnostic {

    public FatalDiagnostic(
        final String message
    ) {
        super(message);
    }

    public FatalDiagnostic(
        final Locale locale,
        final String message
    ) {
        super(locale, message);
    }

    /**
     * Get the level associated with this instance
     */
    @Override
    public Level getLevel(
    ) {
        return Level.Fatal;
    }

    @Override
    public boolean isError() { return true; }
}
