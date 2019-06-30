/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.diagnostics;

import com.kadware.komodo.minalib.Locale;

/**
 * Class for fatal diagnostic messages (situations which kill the assembler - if any)
 */
public class FatalDiagnostic extends Diagnostic {

    public FatalDiagnostic(
        final Locale locale,
        final String message
    ) {
        super(locale, message);
    }

    /**
     * Get the level associated with this instance
     * @return value
     */
    @Override
    public Level getLevel(
    ) {
        return Level.Fatal;
    }
}
