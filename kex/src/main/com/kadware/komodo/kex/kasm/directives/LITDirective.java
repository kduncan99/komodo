/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.LabelFieldComponents;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.TextLine;
import com.kadware.komodo.kex.kasm.diagnostics.WarningDiagnostic;

@SuppressWarnings("Duplicates")
public class LITDirective extends Directive {

    @Override
    public void process(
            final Assembler assembler,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents
    ) {
        if (labelFieldComponents._label != null) {
            Locale loc = new Locale(textLine._lineSpecifier, 1);
            assembler.appendDiagnostic(new WarningDiagnostic(loc, "Label ignored for $LIT directive"));
        }

        if (extractFields(assembler, textLine, false, 2)) {
            assembler.setCurrentLitLCIndex(assembler.getCurrentGenerationLCIndex());
        }
    }
}
