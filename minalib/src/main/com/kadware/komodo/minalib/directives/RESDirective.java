/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.directives;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.expressions.*;
import java.math.BigInteger;

@SuppressWarnings("Duplicates")
public class RESDirective extends Directive {

    @Override
    public void process(
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents
    ) {
        if (labelFieldComponents._label != null) {
            context.establishLabel(labelFieldComponents._labelLocale,
                                   labelFieldComponents._label,
                                   labelFieldComponents._labelLevel,
                                   context.getCurrentLocation());
        }

        if (extractFields(context, textLine, true, 3)) {
            TextSubfield expSubField = _operandField._subfields.get(0);
            String expText = expSubField._text;
            Locale expLocale = expSubField._locale;
            try {
                ExpressionParser p = new ExpressionParser(expText, expLocale);
                Expression e = p.parse(context);
                if (e == null) {
                    context.appendDiagnostic(new ErrorDiagnostic(expLocale, "Syntax error"));
                    return;
                }

                Value v = e.evaluate(context);
                if (!(v instanceof IntegerValue)) {
                    context.appendDiagnostic(new ValueDiagnostic(expLocale, "Wrong value type for $RES operand"));
                    return;
                }

                IntegerValue iv = (IntegerValue) v;
                if (iv._value.get().compareTo(BigInteger.valueOf(0777777)) > 0) {
                    context.appendDiagnostic(new ValueDiagnostic(expLocale, "Value to large for $RES directive"));
                    return;
                }

                long intValue = iv._value.get().longValue();
                if (iv.hasUndefinedReferences()) {
                    context.appendDiagnostic(new RelocationDiagnostic(expLocale));
                }

                context.advanceLocation(context.getCurrentGenerationLCIndex(), (int) intValue);
            } catch (ExpressionException ex) {
                context.appendDiagnostic(new ErrorDiagnostic(expLocale, "Syntax error"));
                return;
            }

            if (_operandField._subfields.size() > 1) {
                Locale loc = _operandField._subfields.get(1)._locale;
                context.appendDiagnostic(new ErrorDiagnostic(loc, "Extraneous subfields ignored"));
            }
        }
    }
}
