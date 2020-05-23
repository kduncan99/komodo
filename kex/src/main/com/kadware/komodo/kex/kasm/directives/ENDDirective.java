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
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;
import com.kadware.komodo.kex.kasm.expressions.ExpressionParser;

@SuppressWarnings("Duplicates")
public class ENDDirective extends Directive {

    @Override
    public void process(
        final Assembler assembler,
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(assembler, textLine, false, 3)) {
            if (_operandField._subfields.size() > 0) {
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

                    assembler.setEndValue(e.evaluate(assembler));
                } catch (ExpressionException ex) {
                    assembler.appendDiagnostic(new ErrorDiagnostic(expLocale, "Syntax error"));
                }
            }

            assembler.setEndFound();
        }
    }
}
