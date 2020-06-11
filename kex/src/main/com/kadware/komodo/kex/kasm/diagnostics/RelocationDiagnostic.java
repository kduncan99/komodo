/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.diagnostics;

import com.kadware.komodo.kex.kasm.Locale;

/**
 * Class for warning diagnostic messages (situations which look wrong, but don't stop us from producing a module)
 */
public class RelocationDiagnostic extends Diagnostic {

    public RelocationDiagnostic(
        final Locale locale
    ) {
        super(locale, "Relocation information lost");
    }

    public RelocationDiagnostic(
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
        return Level.Relocation;
    }

    @Override
    public boolean isError() { return true; }
}
