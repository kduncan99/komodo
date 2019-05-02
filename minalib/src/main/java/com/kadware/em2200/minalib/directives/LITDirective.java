/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.diagnostics.ErrorDiagnostic;
import com.kadware.em2200.minalib.diagnostics.ValueDiagnostic;
import com.kadware.em2200.minalib.dictionary.IntegerValue;
import com.kadware.em2200.minalib.dictionary.Value;
import com.kadware.em2200.minalib.exceptions.ExpressionException;
import com.kadware.em2200.minalib.expressions.Expression;
import com.kadware.em2200.minalib.expressions.ExpressionParser;

@SuppressWarnings("Duplicates")
public class LITDirective implements IDirective {

    @Override
    public String getToken(
    ) {
        return "$LIT";
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
        if (operatorField._subfields.size() > 1) {
            Locale loc = operandField._subfields.get(1)._locale;
            diagnostics.append(new ErrorDiagnostic(loc, "Extraneous subfields ignored"));
        }

        if (labelFieldComponents._label != null) {
            Locale loc = operatorField._locale;
            diagnostics.append(new ErrorDiagnostic(loc, "Label ignored for $LIT directive"));
        }

        if ((operandField != null) && (!operandField._subfields.isEmpty())) {
            Locale loc = operatorField._locale;
            diagnostics.append(new ErrorDiagnostic(loc, "Ignoring operand field for $LIT directive"));
        }

        context._currentLitLCIndex = context._currentGenerationLCIndex;
    }
}
