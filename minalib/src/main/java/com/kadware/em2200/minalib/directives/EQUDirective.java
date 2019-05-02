/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.diagnostics.ErrorDiagnostic;
import com.kadware.em2200.minalib.dictionary.Value;
import com.kadware.em2200.minalib.exceptions.ExpressionException;
import com.kadware.em2200.minalib.expressions.Expression;
import com.kadware.em2200.minalib.expressions.ExpressionParser;

@SuppressWarnings("Duplicates")
public class EQUDirective implements IDirective {

    @Override
    public String getToken(
    ) {
        return "$EQU";
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
        if (labelFieldComponents._label == null) {
            Locale loc = operandField._subfields.get(0)._locale;
            diagnostics.append(new ErrorDiagnostic(loc, "Label required for $EQU directive"));
            return;
        }

        if (operatorField._subfields.size() > 1) {
            Locale loc = operandField._subfields.get(1)._locale;
            diagnostics.append(new ErrorDiagnostic(loc, "Extraneous subfields ignored"));
        }

        if ((operandField == null) || (operandField._subfields.isEmpty())) {
            Locale loc = operatorField._locale;
            diagnostics.append(new ErrorDiagnostic(loc, "Operand required for $EQU directive"));
            return;
        }

        //TODO handle multiple fields as partial words
        TextSubfield expSubField = operandField._subfields.get(0);
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
            context._dictionary.addValue(labelFieldComponents._labelLevel,
                                         labelFieldComponents._label,
                                         v);
        } catch (ExpressionException ex) {
            diagnostics.append(new ErrorDiagnostic(expLocale, "Syntax error"));
            return;
        }
    }
}
