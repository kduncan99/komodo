/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.diagnostics;

import com.kadware.komodo.minalib.Locale;

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
     * <p>
     * @return
     */
    @Override
    public Level getLevel(
    ) {
        return Level.Value;
    }
}
