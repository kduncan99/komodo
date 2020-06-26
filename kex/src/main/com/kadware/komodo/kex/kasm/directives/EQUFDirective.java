/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.CodeMode;
import com.kadware.komodo.kex.kasm.Form;
import com.kadware.komodo.kex.kasm.LabelFieldComponents;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.TextLine;
import com.kadware.komodo.kex.kasm.TextSubfield;
import com.kadware.komodo.kex.kasm.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.EqufValue;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;
import com.kadware.komodo.kex.kasm.expressions.ExpressionParser;
import java.util.Arrays;

@SuppressWarnings("Duplicates")
public class EQUFDirective extends Directive {

    /**
     * Parses the operands into expressions, then evaluates them.
     * Once done, we attempt to fit them into an appropriate form, and store the whole thing
     * as an EqufValue with the various bit fields set according to the expression evaluations.
     * In basic mode, the format of the operands is u,x,j and they are placed into an I$ form.
     * In extended mode, the format is u,x,j,b and they are placed into an EI$ form.
     */

    @Override
    public void process(
        final Assembler assembler,
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(assembler, textLine, true, 3)) {
            if (labelFieldComponents._label == null) {
                Locale loc = new Locale(textLine._lineSpecifier, 1);
                assembler.appendDiagnostic(new ErrorDiagnostic(loc, "Label required for $EQUF directive"));
                return;
            }

            if (_operandField._subfields.isEmpty()) {
                assembler.appendDiagnostic(new ErrorDiagnostic(_operationField._locale, "Syntax error"));
                return;
            }

            boolean basicMode = assembler.getCodeMode() == CodeMode.Basic;
            int maxSubfields = basicMode ? 3 : 4;
            IntegerValue[] fieldValues = new IntegerValue[maxSubfields];
            Arrays.fill(fieldValues, IntegerValue.POSITIVE_ZERO);

            for (int opx = 0; opx < maxSubfields; ++opx) {
                TextSubfield sf = _operandField._subfields.get(opx);
                String expText = sf._text;
                Locale expLocale = sf._locale;

                if (!expText.isEmpty()) {
                    try {
                        boolean good = false;
                        ExpressionParser p = new ExpressionParser(expText, expLocale);
                        Expression e = p.parse(assembler);
                        if (e != null) {
                            Value v = e.evaluate(assembler);
                            if (v instanceof IntegerValue) {
                                fieldValues[opx] = (IntegerValue) v;
                                good = true;
                            }
                        }

                        if (!good) {
                            throw new ExpressionException();
                        }
                    } catch (ExpressionException ex) {
                        assembler.appendDiagnostic(new ErrorDiagnostic(expLocale, "Syntax error"));
                    }
                }
            }

            Form form = basicMode ? Form.I$Form : Form.EI$Form;
            EqufValue equfValue = new EqufValue(_operandField._locale, fieldValues, form);
            assembler.getDictionary().addValue(labelFieldComponents._labelLevel,
                                               labelFieldComponents._label,
                                               labelFieldComponents._labelLocale,
                                               equfValue);
        }
    }
}
