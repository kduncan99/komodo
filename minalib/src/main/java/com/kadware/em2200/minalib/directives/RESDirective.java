/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.*;

@SuppressWarnings("Duplicates")
public class RESDirective implements IDirective {

    @Override
    public String getToken(
    ) {
        return "$RES";
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
        if (labelFieldComponents._label != null) {
            Assembler.establishLabel(labelFieldComponents._labelLocale,
                                     context._dictionary,
                                     labelFieldComponents._label,
                                     labelFieldComponents._labelLevel,
                                     assembler.getCurrentLocation(),
                                     diagnostics);
        }

        if (operatorField._subfields.size() > 1) {
            Locale loc = operandField._subfields.get(1)._locale;
            diagnostics.append(new ErrorDiagnostic(loc, "Extraneous subfields ignored"));
        }

        if ((operandField == null) || (operandField._subfields.isEmpty())) {
            Locale loc = operatorField._locale;
            diagnostics.append(new ErrorDiagnostic(loc, "No word count specified for $RES directive"));
            return;
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
                diagnostics.append(new ValueDiagnostic(expLocale, "Wrong value type for $RES operand"));
                return;
            }

            assembler.advanceLocation(context._currentGenerationLCIndex, (int)((IntegerValue) v)._value);
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
