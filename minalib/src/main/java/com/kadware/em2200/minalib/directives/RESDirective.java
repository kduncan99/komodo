/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.*;

@SuppressWarnings("Duplicates")
public class RESDirective extends Directive {

    @Override
    public void process(
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents,
            final Diagnostics diagnostics
    ) {
        if (labelFieldComponents._label != null) {
            Assembler.establishLabel(labelFieldComponents._labelLocale,
                                     context._dictionary,
                                     labelFieldComponents._label,
                                     labelFieldComponents._labelLevel,
                                     context.getCurrentLocation(),
                                     diagnostics);
        }

        if (extractFields(textLine, true, 3, diagnostics)) {
            TextSubfield expSubField = _operandField._subfields.get(0);
            String expText = expSubField._text;
            Locale expLocale = expSubField._locale;
            try {
                ExpressionParser p = new ExpressionParser(expText, expLocale);
                Expression e = p.parse(context, diagnostics);
                if (e == null) {
                    diagnostics.append(new ErrorDiagnostic(expLocale, "Syntax error"));
                    return;
                }

                Value v = e.evaluate(context, diagnostics);
                if (!(v instanceof IntegerValue) || (((IntegerValue) v)._undefinedReferences.length != 0)) {
                    diagnostics.append(new ValueDiagnostic(expLocale, "Wrong value type for $RES operand"));
                    return;
                }

                context.advanceLocation(context._currentGenerationLCIndex, (int) ((IntegerValue) v)._value);
            } catch (ExpressionException ex) {
                diagnostics.append(new ErrorDiagnostic(expLocale, "Syntax error"));
                return;
            }

            if (_operandField._subfields.size() > 1) {
                Locale loc = _operandField._subfields.get(1)._locale;
                diagnostics.append(new ErrorDiagnostic(loc, "Extraneous subfields ignored"));
            }
        }
    }
}
