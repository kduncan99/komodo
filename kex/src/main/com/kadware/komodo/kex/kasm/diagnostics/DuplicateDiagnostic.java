/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.diagnostics;

import com.kadware.komodo.kex.kasm.Locale;

/**
 * Class for duplicate label diagnostic messages (situations which prevent the production of a complete module)
 */
public class DuplicateDiagnostic extends Diagnostic {

    public DuplicateDiagnostic(
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
        return Level.Duplicate;
    }

    @Override
    public boolean isError() { return true; }
}
