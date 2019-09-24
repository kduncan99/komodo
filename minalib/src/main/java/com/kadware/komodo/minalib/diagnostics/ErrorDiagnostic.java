/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.diagnostics;

import com.kadware.komodo.minalib.Locale;

/**
 * Class for error diagnostic messages (situations which prevent the production of a complete module)
 */
public class ErrorDiagnostic extends Diagnostic {

    public ErrorDiagnostic(
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
        return Level.Error;
    }
}
