/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.directives;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.expressions.*;

@SuppressWarnings("Duplicates")
public class GFORMDirective extends Directive {

    @Override
    public void process(
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(context, textLine, true, 3)) {
            if ((_operandField._subfields.size() % 2) != 0) {
                Locale loc = _operationField._locale;
                context.appendDiagnostic(new ErrorDiagnostic(loc,
                                                             "Even number of operands required for $GFORM directive"));
                return;
            }

            //  There are 2n subfields, indicating n fields to be generated.
            //  The even-numbered subfields starting with 0, indicate field size, while the odd-number subfields
            //  contain expressions which are intended to fit inside the field defined by each expression's
            //  preceding field size.

            int entities = _operandField._subfields.size() >> 1;
            int[] fieldSizes = new int[entities];
            IntegerValue[] values = new IntegerValue[entities];
            int enx = 0;
            int fsx = 0;
            int vx = 1;
            boolean error = false;
            while (vx < _operandField._subfields.size()) {
                TextSubfield sfFieldSize = _operandField._subfields.get(fsx);
                TextSubfield sfValue = _operandField._subfields.get(vx);
                ExpressionParser epFieldSize = new ExpressionParser(sfFieldSize._text, sfFieldSize._locale);
                ExpressionParser epValue = new ExpressionParser(sfValue._text, sfValue._locale);

                try {
                    Expression e = epFieldSize.parse(context);
                    if (e == null) {
                        context.appendDiagnostic(new ErrorDiagnostic(sfFieldSize._locale,
                                                                     "Expected an expression for field size"));
                        error = true;
                    } else {
                        Value v = e.evaluate(context);
                        if (!(v instanceof IntegerValue)) {
                            context.appendDiagnostic(new ValueDiagnostic(sfFieldSize._locale,
                                                                         "Invalid value for field size"));
                            error = true;
                        } else {
                            IntegerValue iv = (IntegerValue) v;
                            if ((iv._undefinedReferences.length > 0) || (iv._value <= 0) || (iv._value > 36)) {
                                context.appendDiagnostic(new ValueDiagnostic(sfFieldSize._locale,
                                                                             "Invalid value for field size"));
                                error = true;
                            } else {
                                fieldSizes[enx] = (int) iv._value;
                            }
                        }
                    }
                } catch (ExpressionException ex) {
                    context.appendDiagnostic(new ErrorDiagnostic(sfFieldSize._locale,
                                                                 "Syntax error in field size"));
                    error = true;
                }

                try {
                    Expression e = epValue.parse(context);
                    if (e == null) {
                        context.appendDiagnostic(new ErrorDiagnostic(sfFieldSize._locale,
                                                                     "Expected a value expression"));
                        error = true;
                    } else {
                        Value v = e.evaluate(context);
                        if (!(v instanceof IntegerValue)) {
                            context.appendDiagnostic(new ValueDiagnostic(sfFieldSize._locale,
                                                                         "Invalid value for field size"));
                            error = true;
                        } else {
                            values[enx] = (IntegerValue) v;
                        }
                    }
                } catch (ExpressionException ex) {
                    context.appendDiagnostic(new ErrorDiagnostic(sfFieldSize._locale,
                                                                 "Syntax error in value expression"));
                    error = true;
                }

                ++enx;
                fsx += 2;
                vx += 2;
            }

            if (!error) {
                if (labelFieldComponents._label != null) {
                    context.establishLabel(labelFieldComponents._labelLocale,
                                           labelFieldComponents._label,
                                           labelFieldComponents._labelLevel,
                                           context.getCurrentLocation());
                }

                context.generate(_operandField._locale.getLineSpecifier(),
                                 context.getCurrentGenerationLCIndex(),
                                 new Form(fieldSizes),
                                 values);
            }
        }
    }
}
