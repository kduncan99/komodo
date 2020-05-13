/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.diagnostics;

import com.kadware.komodo.kex.kasm.Locale;

/**
 * Class for error diagnostic messages (situations which prevent the production of a complete module)
 */
public class QuoteDiagnostic extends Diagnostic {

    public QuoteDiagnostic(
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
        return Level.Quote;
    }

    @Override
    public boolean isError() { return true; }
}
