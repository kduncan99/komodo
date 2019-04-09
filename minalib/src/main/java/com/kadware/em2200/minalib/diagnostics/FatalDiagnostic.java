/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.diagnostics;

import com.kadware.em2200.minalib.Locale;

/**
 * Class for fatal diagnostic messages (situations which kill the assembler - if any)
 */
public abstract class FatalDiagnostic extends Diagnostic {

    public FatalDiagnostic(
        final Locale locale,
        final String message
    ) {
        super(locale, message);
    }

    /**
     * Get the level associated with this instance
     * <p>
     * @return
     */
    @Override
    public Level getLevel(
    ) {
        return Level.Fatal;
    }
}
