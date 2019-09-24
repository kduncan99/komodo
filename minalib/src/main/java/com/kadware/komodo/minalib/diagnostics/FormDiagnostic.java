/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.diagnostics;

import com.kadware.komodo.minalib.Locale;

/**
 * Something wrong with operands which have incompatible forms
 */
public class FormDiagnostic extends Diagnostic {

    public FormDiagnostic(
        final Locale locale
    ) {
        super(locale, "Incompatible attached forms");
    }

    public FormDiagnostic(
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
        return Level.Form;
    }
}
