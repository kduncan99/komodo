/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.directives;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.minalib.diagnostics.FormDiagnostic;
import com.kadware.komodo.minalib.diagnostics.RelocationDiagnostic;
import com.kadware.komodo.minalib.diagnostics.ValueDiagnostic;
import com.kadware.komodo.minalib.dictionary.FormValue;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.exceptions.InvalidParameterException;
import com.kadware.komodo.minalib.expressions.Expression;
import com.kadware.komodo.minalib.expressions.ExpressionParser;
import java.math.BigInteger;

@SuppressWarnings("Duplicates")
public class FORMDirective extends Directive {

    @Override
    public void process(
        final Context context,
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(context, textLine, true, 3)) {
            if (labelFieldComponents._label == null) {
                Locale loc = new Locale(textLine._lineSpecifier, 1);
                context.appendDiagnostic(new ErrorDiagnostic(loc, "Label required for $FORM directive"));
                return;
            }

            boolean err = false;
            int[] fieldSizes = new int[_operandField._subfields.size()];
            int count = 0;
            for (int sfx = 0; sfx < _operandField._subfields.size(); ++sfx) {
                TextSubfield sf = _operandField._subfields.get(sfx);
                String sfText = sf._text;
                Locale sfLocale = sf._locale;
                try {
                    ExpressionParser p = new ExpressionParser(sfText, sfLocale);
                    Expression e = p.parse(context);
                    if (e == null) {
                        context.appendDiagnostic(new ErrorDiagnostic(sfLocale, "Syntax error"));
                        err = true;
                    } else {
                        Value ev = e.evaluate(context);
                        if (ev instanceof IntegerValue) {
                            IntegerValue iv = (IntegerValue) ev;
                            if (iv._form != null) {
                                context.appendDiagnostic(new FormDiagnostic(sfLocale));
                                err = true;
                            }

                            if (iv.hasUndefinedReferences()) {
                                context.appendDiagnostic(new RelocationDiagnostic(sfLocale));
                                err = true;
                            }

                            if ((iv._value.get().compareTo(BigInteger.ONE) < 0)
                                || (iv._value.get().compareTo(BigInteger.valueOf(36)) > 0)) {
                                context.appendDiagnostic(new ValueDiagnostic(sfLocale, "Invalid value for field size"));
                                err = true;
                            } else {
                                fieldSizes[sfx] = iv._value.get().intValue();
                                count += fieldSizes[sfx];
                            }
                        } else {
                            context.appendDiagnostic(new ValueDiagnostic(sfLocale, "Wrong value type for field size"));
                            err = true;
                        }
                    }
                } catch (ExpressionException ex) {
                    context.appendDiagnostic(new ErrorDiagnostic(sfLocale, "Syntax error"));
                }
            }

            if (count > 36) {
                context.appendDiagnostic(new FormDiagnostic(_operandField._locale, "Cumulative form size > 36 bits"));
                err = true;
            }

            if (!err) {
                context.getDictionary().addValue(labelFieldComponents._labelLevel,
                                                 labelFieldComponents._label,
                                                 new FormValue.Builder().setForm(new Form(fieldSizes)).build());
            }
        }
    }
}
