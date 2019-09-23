/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.directives;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.expressions.*;

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
                Locale loc = new Locale(textLine._lineSpecifier, 1);
                context.appendDiagnostic(new ErrorDiagnostic(loc, "Label required for $EQU directive"));
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
                    context.appendDiagnostic(new ErrorDiagnostic(expLocale, "Syntax error"));
                    return;
                }

                context.getDictionary().addValue(labelFieldComponents._labelLevel,
                                                 labelFieldComponents._label,
                                                 e.evaluate(context));
            } catch (ExpressionException ex) {
                context.appendDiagnostic(new ErrorDiagnostic(expLocale, "Syntax error"));
            }
        }
    }
}
