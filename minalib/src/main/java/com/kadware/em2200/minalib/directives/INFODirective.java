/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;

@SuppressWarnings("Duplicates")
public class INFODirective extends Directive {

    @Override
    public void process(
            final Assembler assembler,
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents,
            final Diagnostics diagnostics
    ) {
        if (extractFields(textLine, true, 4, diagnostics)) {
            //TODO
        }
    }
}
