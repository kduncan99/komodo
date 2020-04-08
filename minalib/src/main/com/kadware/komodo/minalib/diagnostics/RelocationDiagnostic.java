/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.diagnostics;

import com.kadware.komodo.minalib.Locale;

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
     * <p>
     * @return
     */
    @Override
    public Level getLevel(
    ) {
        return Level.Relocation;
    }
}
