/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.diagnostics;

import com.kadware.komodo.kex.kasm.Locale;

/**
 * Class for reporting warnings which might indicate a problem, but do not prevent producing a module
 */
public class WarningDiagnostic extends Diagnostic {

    public WarningDiagnostic(
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
        return Level.Warning;
    }

    @Override
    public boolean isError() { return false; }
}
