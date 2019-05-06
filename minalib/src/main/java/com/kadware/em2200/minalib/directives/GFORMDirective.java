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
public class GFORMDirective implements IDirective {

    @Override
    public String getToken(
    ) {
        return "$GFORM";
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

        if ((operandField == null) || (operandField._subfields.isEmpty()) || ((operandField._subfields.size() % 2) != 0)) {
            Locale loc = operatorField._locale;
            diagnostics.append(new ErrorDiagnostic(loc, "Even number of operands required for $GFORM directive"));
            return;
        }

        //  There are 2n subfields, indicating n fields to be generated.
        //  The even-numbered subfields starting with 0, indicate field size, while the odd-number subfields
        //  contain expressions which are intended to fit inside the field defined by each expression's
        //  preceding field size.

        int entities = operandField._subfields.size() >> 1;
        int[] fieldSizes = new int[entities];
        IntegerValue[] values = new IntegerValue[entities];
        int enx = 0;
        int fsx = 0;
        int vx = 1;
        boolean error = false;
        while (vx < operandField._subfields.size()) {
            TextSubfield sfFieldSize = operandField._subfields.get(fsx);
            TextSubfield sfValue = operandField._subfields.get(vx);
            ExpressionParser epFieldSize = new ExpressionParser(sfFieldSize._text, sfFieldSize._locale);
            ExpressionParser epValue = new ExpressionParser(sfValue._text, sfValue._locale);

            try {
                Expression e = epFieldSize.parse(context, diagnostics);
                if (e == null) {
                    diagnostics.append(new ErrorDiagnostic(sfFieldSize._locale,
                                                           "Expected an expression for field size"));
                    error = true;
                } else {
                    Value v = e.evaluate(context, diagnostics);
                    if ( !(v instanceof IntegerValue) ) {
                        diagnostics.append(new ValueDiagnostic(
                            sfFieldSize._locale,
                            "Invalid value for field size"));
                        error = true;
                    } else {
                        IntegerValue iv = (IntegerValue) v;
                        if ( (iv._undefinedReferences.length > 0) || (iv._value <= 0) || (iv._value > 36) ) {
                            diagnostics.append(new ValueDiagnostic(
                                sfFieldSize._locale,
                                "Invalid value for field size"));
                            error = true;
                        } else {
                            fieldSizes[enx] = (int) iv._value;
                        }
                    }
                }
            } catch (ExpressionException ex) {
                diagnostics.append(new ErrorDiagnostic(sfFieldSize._locale,
                                                       "Syntax error in field size"));
                error = true;
            }

            try {
                Expression e = epValue.parse(context, diagnostics);
                if (e == null) {
                    diagnostics.append(new ErrorDiagnostic(sfFieldSize._locale,
                                                           "Expected a value expression"));
                    error = true;
                } else {
                    Value v = e.evaluate(context, diagnostics);
                    if (!(v instanceof IntegerValue)) {
                        diagnostics.append(new ValueDiagnostic(
                            sfFieldSize._locale,
                            "Invalid value for field size"));
                        error = true;
                    } else {
                        values[enx] = (IntegerValue) v;
                    }
                }
            } catch (ExpressionException ex) {
                diagnostics.append(new ErrorDiagnostic(sfFieldSize._locale,
                                                       "Syntax error in value expression"));
                error = true;
            }

            ++enx;
            fsx += 2;
            vx += 2;
        }

        if (!error) {
            if (labelFieldComponents._label != null) {
                Assembler.establishLabel(labelFieldComponents._labelLocale,
                                         context._dictionary,
                                         labelFieldComponents._label,
                                         labelFieldComponents._labelLevel,
                                         context.getCurrentLocation(),
                                         diagnostics);
            }

            context.generate(operandField._locale,
                             context._currentGenerationLCIndex,
                             new Form(fieldSizes),
                             values);
        }
    }
}
