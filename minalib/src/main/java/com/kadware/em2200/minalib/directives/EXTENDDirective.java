/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;

@SuppressWarnings("Duplicates")
public class EXTENDDirective implements IDirective {

    @Override
    public String getToken(
    ) {
        return "$EXTEND";
    }

    @Override
    public void process(
            final Assembler assembler,
            final Context context,
            final LabelFieldComponents labelFieldComponents,
            final TextField operatorField,
            final TextField operandField,
            final Diagnostics diagnostics
    ) {
        context._codeMode = CodeMode.Extended;
    }
}
