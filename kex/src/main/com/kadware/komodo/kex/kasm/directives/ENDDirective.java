/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.baselib.exceptions.NotFoundException;
import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.AssemblerOption;
import com.kadware.komodo.kex.kasm.LabelFieldComponents;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.TextLine;
import com.kadware.komodo.kex.kasm.TextSubfield;
import com.kadware.komodo.kex.kasm.UnresolvedReference;
import com.kadware.komodo.kex.kasm.UnresolvedReferenceToLabel;
import com.kadware.komodo.kex.kasm.UnresolvedReferenceToLocationCounter;
import com.kadware.komodo.kex.kasm.diagnostics.Diagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.RelocationDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.ValueDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.WarningDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.Dictionary;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
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
            //  If there is an operand, parse and evaluate it.
            //  For PROC subassemblies, we subsequently ignore it (with a diagnostic, though)
            //  For FUNC subassemblies, we store it without further ado
            //  For main assemblies, we ensure the value is suitable for use as a code entry point.
            if ((_operandField != null) && (_operandField._subfields.size() > 0)) {
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

                    Value v = e.evaluate(assembler);

                    if (assembler.isOptionSet(AssemblerOption.DEFINITION_MODE)) {
                        String msg = "$END cannot have an operand in definition mode";
                        assembler.appendDiagnostic(new ErrorDiagnostic(expLocale, msg));
                    } else {
                        if (assembler.isProcedureSubAssembly()) {
                            assembler.appendDiagnostic(new WarningDiagnostic(expLocale, "Return value for $PROC ignored"));
                        } else if (assembler.isFunctionSubAssembly()) {
                            assembler.setEndValue(v);
                        } else if (assembler.isMainAssembly()) {
                            resolveEntryPoint(assembler, v, expLocale);
                        }
                    }
                } catch (ExpressionException ex) {
                    assembler.appendDiagnostic(new ErrorDiagnostic(expLocale, "Syntax error"));
                }
            }

            //  Tell the controlling (sub)assembly that we've hit an $END...
            //  For sub assemblies this will always be the end of the source as well (because we make it that way).
            //  For main assemblies the assembler will need to know this in order to flag extraneous code
            assembler.setEndFound();
        }
    }

    private void resolveEntryPoint(
        final Assembler assembler,
        final Value expressionValue,
        final Locale expressionLocale
    ) {
        if (!(expressionValue instanceof IntegerValue)) {
            Diagnostic d = new ValueDiagnostic(expressionLocale, "Wrong value type for entry point");
            assembler.appendDiagnostic(d);
            return;
        }

        IntegerValue iv = (IntegerValue) expressionValue;
        if (iv._references.length == 0) {
            //  absolute value - this is acceptable
            assembler.setEndValue(iv);
        } else {
            if (iv._references.length > 1) {
                Diagnostic d = new RelocationDiagnostic(expressionLocale,
                                                        "Improper relocation for entry point");
                assembler.appendDiagnostic(d);
                return;
            }

            UnresolvedReference ur = iv._references[0];
            if (ur instanceof UnresolvedReferenceToLocationCounter) {
                //  This also is acceptable
                assembler.setEndValue(iv);
            } else if (ur instanceof UnresolvedReferenceToLabel) {
                //  If it's a label, resolve it.  If we cannot resolve it, it's not local and we can't use it.
                try {
                    UnresolvedReferenceToLabel url = (UnresolvedReferenceToLabel) ur;
                    Dictionary.ValueInfo vi = assembler.getDictionary().getValueInfo(url._label);
                    assembler.setEndValue(vi._value);
                } catch (NotFoundException ex) {
                    Diagnostic d = new ValueDiagnostic(expressionLocale,
                                                       "Cannot use an undefined reference as an entry point");
                    assembler.appendDiagnostic(d);
                }
            }
        }
    }
}
