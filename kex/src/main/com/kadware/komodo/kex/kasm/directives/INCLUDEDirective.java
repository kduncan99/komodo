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
import com.kadware.komodo.kex.kasm.diagnostics.WarningDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.StringValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;
import com.kadware.komodo.kex.kasm.expressions.ExpressionParser;

@SuppressWarnings("Duplicates")
public class INCLUDEDirective extends Directive {

    @Override
    public void process(
        final Assembler assembler,
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(assembler, textLine, true, 3)) {
            if (labelFieldComponents._label != null) {
                assembler.appendDiagnostic(new WarningDiagnostic(labelFieldComponents._labelLocale,
                                                                 "Label ignored for $INCLUDE directive"));
            }

            TextSubfield sf = _operandField._subfields.get(0);
            String sfText = sf._text;
            Locale sfLocale = sf._locale;
            try {
                ExpressionParser p = new ExpressionParser(sfText, sfLocale);
                Expression e = p.parse(assembler);
                if (e == null) {
                    assembler.appendDiagnostic(new ErrorDiagnostic(sfLocale, "Syntax error"));
                } else {
                    Value ev = e.evaluate(assembler);
                    if (ev instanceof StringValue) {
                        assembler.importDefinitions(((StringValue) ev)._value, sfLocale);
                    }
                }
            } catch (ExpressionException ex) {
                assembler.appendDiagnostic(new ErrorDiagnostic(sfLocale, "Syntax error"));
            }
        }
    }
}
