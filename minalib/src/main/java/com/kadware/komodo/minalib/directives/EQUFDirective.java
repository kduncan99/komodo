/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.directives;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.expressions.Expression;
import com.kadware.komodo.minalib.expressions.ExpressionParser;

@SuppressWarnings("Duplicates")
public class EQUFDirective extends Directive {

    /**
     * Parses the operands into expressions, then evaluates them.
     * Once done, we attempt to fit them into an appropriate form, and store the whole thing
     * as an IntegerValue with the form and any appropriate undefined references attached thereto.
     * @param context reference to the context in which this directive is to execute
     * @param textLine contains the basic parse into fields/subfields - we cannot drill down further, as various directives
     *                 make different usages of the fields - and $INFO even uses an extra field
     * @param labelFieldComponents LabelFieldComponents describing the label field on the line containing this directive
     */

    private IntegerValue _value = null;

    @Override
    public void process(
        final Context context,
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(context, textLine, true, 3)) {
            if (labelFieldComponents._label == null) {
                Locale loc = new Locale(textLine._lineSpecifier, 1);
                context.appendDiagnostic(new ErrorDiagnostic(loc, "Label required for $EQUF directive"));
                return;
            }

            int maxSubfields = context.getCodeMode() == CodeMode.Basic ? 3 : 4;
            if (_operandField._subfields.isEmpty() || _operandField._subfields.size() > maxSubfields) {
                context.appendDiagnostic(new ErrorDiagnostic(_operationField._locale, "Syntax error"));
            }

            //TODO
//            Form form = context.getCodeMode() == CodeMode.Basic ? Form.I$Form : Form.EI$Form;
//            IntegerValue[] fieldValues = new IntegerValue[_operandField._subfields.size()];
//            for (int fvx = 0; fvx < fieldValues.length; ++fvx) {
//                fieldValues[fvx] = _zero;
//            }
//
//            int fvx = 0;
//            for (TextSubfield sf : _operandField._subfields) {
//                String expText = sf._text;
//                Locale expLocale = sf._locale;
//                if (!expText.isEmpty()) {
//                    try {
//                        boolean good = false;
//                        ExpressionParser p = new ExpressionParser(expText, expLocale);
//                        Expression e = p.parse(context);
//                        if (e != null) {
//                            Value v = e.evaluate(context);
//                            if (v instanceof IntegerValue) {
//                                fieldValues[fvx] = (IntegerValue) v;
//                                good = true;
//                            }
//                        }
//
//                        if (!good) {
//                            context.appendDiagnostic(new ErrorDiagnostic(expLocale, "Syntax error"));
//                        }
//                    } catch (ExpressionException ex) {
//                        context.appendDiagnostic(new ErrorDiagnostic(expLocale, "Syntax error"));
//                    }
//                }
//
//                ++fvx;
//            }
        }
    }
}
