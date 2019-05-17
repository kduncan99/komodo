/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.ErrorDiagnostic;

@SuppressWarnings("Duplicates")
public class LITDirective extends Directive {

    @Override
    public void process(
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents
    ) {
        if (labelFieldComponents._label != null) {
            Locale loc = new Locale(textLine._lineSpecifier, 1);
            context.appendDiagnostic(new ErrorDiagnostic(loc, "Label ignored for $LIT directive"));
        }

        if (extractFields(context, textLine, false, 2)) {
            context.setCurrentLitLCIndex(context.getCurrentGenerationLCIndex());
        }
    }
}
