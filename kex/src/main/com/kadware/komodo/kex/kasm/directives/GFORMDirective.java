/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.diagnostics.*;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.*;
import java.math.BigInteger;

@SuppressWarnings("Duplicates")
public class GFORMDirective extends Directive {

    @Override
    public void process(
        final Assembler assembler,
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(assembler, textLine, true, 3)) {
            if ((_operandField._subfields.size() % 2) != 0) {
                Locale loc = _operationField._locale;
                assembler.appendDiagnostic(new ErrorDiagnostic(loc,
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
                    Expression e = epFieldSize.parse(assembler);
                    if (e == null) {
                        assembler.appendDiagnostic(new ErrorDiagnostic(sfFieldSize._locale,
                                                                       "Expected an expression for field size"));
                        error = true;
                    } else {
                        Value v = e.evaluate(assembler);
                        if (!(v instanceof IntegerValue)) {
                            assembler.appendDiagnostic(new ValueDiagnostic(sfFieldSize._locale,
                                                                           "Invalid value for field size"));
                            error = true;
                        } else {
                            IntegerValue iv = (IntegerValue) v;
                            if ((iv.hasUndefinedReferences())
                                || (iv._value.get().compareTo(BigInteger.ZERO) <= 0)
                                || (iv._value.get().compareTo(BigInteger.valueOf(36)) > 0)) {
                                assembler.appendDiagnostic(new ValueDiagnostic(sfFieldSize._locale,
                                                                               "Invalid value for field size"));
                                error = true;
                            } else {
                                fieldSizes[enx] = iv._value.get().intValue();
                            }
                        }
                    }
                } catch (ExpressionException ex) {
                    assembler.appendDiagnostic(new ErrorDiagnostic(sfFieldSize._locale,
                                                                   "Syntax error in field size"));
                    error = true;
                }

                try {
                    Expression e = epValue.parse(assembler);
                    if (e == null) {
                        assembler.appendDiagnostic(new ErrorDiagnostic(sfFieldSize._locale,
                                                                       "Expected a value expression"));
                        error = true;
                    } else {
                        Value v = e.evaluate(assembler);
                        if (!(v instanceof IntegerValue)) {
                            assembler.appendDiagnostic(new ValueDiagnostic(sfFieldSize._locale,
                                                                           "Invalid value for field size"));
                            error = true;
                        } else {
                            values[enx] = (IntegerValue) v;
                        }
                    }
                } catch (ExpressionException ex) {
                    assembler.appendDiagnostic(new ErrorDiagnostic(sfFieldSize._locale,
                                                                   "Syntax error in value expression"));
                    error = true;
                }

                ++enx;
                fsx += 2;
                vx += 2;
            }

            if (!error) {
                if (labelFieldComponents._label != null) {
                    assembler.establishLabel(labelFieldComponents._labelLocale,
                                             labelFieldComponents._label,
                                             labelFieldComponents._labelLevel,
                                             assembler.getCurrentLocation());
                }

                assembler.generate(_operandField._locale, assembler.getCurrentGenerationLCIndex(), new Form(fieldSizes), values);
            }
        }
    }
}
