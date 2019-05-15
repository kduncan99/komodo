/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.ErrorDiagnostic;
import com.kadware.em2200.minalib.exceptions.ExpressionException;
import com.kadware.em2200.minalib.expressions.Expression;
import com.kadware.em2200.minalib.expressions.ExpressionParser;

@SuppressWarnings("Duplicates")
public class EQUDirective extends Directive {

    @Override
    public void process(
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(context, textLine, true, 3)) {
            if (labelFieldComponents._label == null) {
                Locale loc = new Locale(textLine._lineNumber, 1);
                context._diagnostics.append(new ErrorDiagnostic(loc, "Label required for $EQU directive"));
                return;
            }

            //TODO handle multiple fields as partial words
            TextSubfield expSubField = _operandField._subfields.get(0);
            String expText = expSubField._text;
            Locale expLocale = expSubField._locale;
            try {
                ExpressionParser p = new ExpressionParser(expText, expLocale);
                Expression e = p.parse(context);
                if (e == null) {
                    context._diagnostics.append(new ErrorDiagnostic(expLocale, "Syntax error"));
                    return;
                }

                context._dictionary.addValue(labelFieldComponents._labelLevel,
                                             labelFieldComponents._label,
                                             e.evaluate(context));
            } catch (ExpressionException ex) {
                context._diagnostics.append(new ErrorDiagnostic(expLocale, "Syntax error"));
            }
        }
    }
}
