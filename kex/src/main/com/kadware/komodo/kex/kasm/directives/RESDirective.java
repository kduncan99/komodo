/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.LabelFieldComponents;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.TextLine;
import com.kadware.komodo.kex.kasm.TextSubfield;
import com.kadware.komodo.kex.kasm.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.RelocationDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.ValueDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;
import com.kadware.komodo.kex.kasm.expressions.ExpressionParser;
import java.math.BigInteger;

@SuppressWarnings("Duplicates")
public class RESDirective extends Directive {

    @Override
    public void process(
            final Assembler assembler,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents
    ) {
        if (labelFieldComponents._label != null) {
            assembler.establishLabel(labelFieldComponents._labelLocale,
                                     labelFieldComponents._label,
                                     labelFieldComponents._labelLevel,
                                     assembler.getCurrentLocation());
        }

        if (extractFields(assembler, textLine, true, 3)) {
            TextSubfield expSubField = _operandField._subfields.get(0);
            String expText = expSubField._text;
            Locale expLocale = expSubField._locale;
            try {
                ExpressionParser p = new ExpressionParser(expText, expLocale);
                Expression e = p.parse(assembler);
                if (e == null) {
                    assembler.appendDiagnostic(new ErrorDiagnostic(expLocale, "Syntax error"));
                    return;
                }

                Value v = e.evaluate(assembler);
                if (!(v instanceof IntegerValue)) {
                    assembler.appendDiagnostic(new ValueDiagnostic(expLocale, "Wrong value type for $RES operand"));
                    return;
                }

                IntegerValue iv = (IntegerValue) v;
                if (iv._value.get().compareTo(BigInteger.valueOf(0777777)) > 0) {
                    assembler.appendDiagnostic(new ValueDiagnostic(expLocale, "Value to large for $RES directive"));
                    return;
                }

                long intValue = iv._value.get().longValue();
                if (iv.hasUndefinedReferences()) {
                    assembler.appendDiagnostic(new RelocationDiagnostic(expLocale));
                }

                assembler.getGeneratedPools().advance(assembler.getCurrentGenerationLCIndex(), (int) intValue);
            } catch (ExpressionException ex) {
                assembler.appendDiagnostic(new ErrorDiagnostic(expLocale, "Syntax error"));
                return;
            }

            if (_operandField._subfields.size() > 1) {
                Locale loc = _operandField._subfields.get(1)._locale;
                assembler.appendDiagnostic(new ErrorDiagnostic(loc, "Extraneous subfields ignored"));
            }
        }
    }
}
