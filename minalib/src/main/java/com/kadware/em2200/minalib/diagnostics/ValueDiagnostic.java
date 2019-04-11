/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.diagnostics;

import com.kadware.em2200.minalib.Locale;

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
