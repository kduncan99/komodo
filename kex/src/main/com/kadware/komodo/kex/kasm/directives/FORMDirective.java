/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.FormDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.RelocationDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.ValueDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.FormValue;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;
import com.kadware.komodo.kex.kasm.expressions.ExpressionParser;
import java.math.BigInteger;

@SuppressWarnings("Duplicates")
public class FORMDirective extends Directive {

    @Override
    public void process(
        final Assembler assembler,
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(assembler, textLine, true, 3)) {
            if (labelFieldComponents._label == null) {
                Locale loc = new Locale(textLine._lineSpecifier, 1);
                assembler.appendDiagnostic(new ErrorDiagnostic(loc, "Label required for $FORM directive"));
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
                    Expression e = p.parse(assembler);
                    if (e == null) {
                        assembler.appendDiagnostic(new ErrorDiagnostic(sfLocale, "Syntax error"));
                        err = true;
                    } else {
                        Value ev = e.evaluate(assembler);
                        if (ev instanceof IntegerValue) {
                            IntegerValue iv = (IntegerValue) ev;
                            if (iv._form != null) {
                                assembler.appendDiagnostic(new FormDiagnostic(sfLocale));
                                err = true;
                            }

                            if (iv.hasUndefinedReferences()) {
                                assembler.appendDiagnostic(new RelocationDiagnostic(sfLocale));
                                err = true;
                            }

                            if ((iv._value.get().compareTo(BigInteger.ONE) < 0)
                                || (iv._value.get().compareTo(BigInteger.valueOf(36)) > 0)) {
                                assembler.appendDiagnostic(new ValueDiagnostic(sfLocale, "Invalid value for field size"));
                                err = true;
                            } else {
                                fieldSizes[sfx] = iv._value.get().intValue();
                                count += fieldSizes[sfx];
                            }
                        } else {
                            assembler.appendDiagnostic(new ValueDiagnostic(sfLocale, "Wrong value type for field size"));
                            err = true;
                        }
                    }
                } catch (ExpressionException ex) {
                    assembler.appendDiagnostic(new ErrorDiagnostic(sfLocale, "Syntax error"));
                }
            }

            if (count > 36) {
                assembler.appendDiagnostic(new FormDiagnostic(_operandField._locale, "Cumulative form size > 36 bits"));
                err = true;
            }

            if (!err) {
                FormValue fValue = new FormValue(_operandField._locale, new Form(fieldSizes));
                assembler.getDictionary().addValue(labelFieldComponents._labelLevel,
                                                   labelFieldComponents._label,
                                                   labelFieldComponents._labelLocale,
                                                   fValue);
            }
        }
    }
}
