/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.diagnostics;

import com.kadware.komodo.kex.kasm.Locale;

/**
 * Class for reporting invalid value diagnostic messages
 */
public class ValueDiagnostic extends Diagnostic {

    public ValueDiagnostic(
        final Locale locale,
        final String reference
    ) {
        super(locale, reference);
    }

    /**
     * Get the level associated with this instance
     */
    @Override
    public Level getLevel(
    ) {
        return Level.Value;
    }

    @Override
    public boolean isError() { return true; }
}
