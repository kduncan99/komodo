/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;

@SuppressWarnings("Duplicates")
public class EXTENDDirective extends Directive {

    @Override
    public void process(
            final Assembler assembler,
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents,
            final Diagnostics diagnostics
    ) {
        if (extractFields(textLine, false, 2, diagnostics)) {
            context._codeMode = CodeMode.Extended;
        }
    }
}
