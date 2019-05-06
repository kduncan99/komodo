/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.diagnostics.ErrorDiagnostic;

@SuppressWarnings("Duplicates")
public class LITDirective extends Directive {

    @Override
    public void process(
            final Assembler assembler,
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents,
            final Diagnostics diagnostics
    ) {
        if (labelFieldComponents._label != null) {
            Locale loc = new Locale(textLine._lineNumber, 1);
            diagnostics.append(new ErrorDiagnostic(loc, "Label ignored for $LIT directive"));
        }

        if (extractFields(textLine, false, 2, diagnostics)) {
            context._currentLitLCIndex = context._currentGenerationLCIndex;
        }
    }
}
