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

        if ((operandField == null) || (operandField._subfields.isEmpty())) {
            Locale loc = operatorField._locale;
            diagnostics.append(new ErrorDiagnostic(loc, "No location counter index specified for $LIT directive"));
            return;
        }

        if (labelFieldComponents._label != null) {
            Locale loc = operatorField._locale;
            diagnostics.append(new ErrorDiagnostic(loc, "Label ignored for $LIT directive"));
        }

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
            if (!(v instanceof IntegerValue) || (((IntegerValue) v)._undefinedReferences.length != 0)) {
                diagnostics.append(new ValueDiagnostic(expLocale, "Wrong value type for $LIT operand"));
                return;
            }

            IntegerValue ival = (IntegerValue) v;
            if ((ival._value < 0) || (ival._value > 63)) {
                diagnostics.append(new ValueDiagnostic(expLocale, "Invalid value for $LIT operand"));
                return;
            }

            context._currentLitLCIndex = (int) ival._value;
        } catch (ExpressionException ex) {
            diagnostics.append(new ErrorDiagnostic(expLocale, "Syntax error"));
            return;
        }

        if (operandField._subfields.size() > 1) {
            Locale loc = operandField._subfields.get(1)._locale;
            diagnostics.append(new ErrorDiagnostic(loc, "Extraneous subfields ignored"));
        }
    }
}
