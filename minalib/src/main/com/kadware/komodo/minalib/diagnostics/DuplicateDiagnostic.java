/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.diagnostics;

import com.kadware.komodo.minalib.Locale;

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
     * @return level
     */
    @Override
    public Level getLevel(
    ) {
        return Level.Duplicate;
    }
}
