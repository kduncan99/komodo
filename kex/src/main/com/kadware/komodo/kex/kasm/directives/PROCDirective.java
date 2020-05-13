/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.diagnostics.*;
import com.kadware.komodo.kex.kasm.dictionary.ProcedureValue;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class PROCDirective extends Directive {

    /**
     * Checks the textline for nest level change.
     * If the textline is $PROC or $FUNC, we return 1 to indicate a further level of nesting.
     * If the textline is an $END directive, we return -1 to indicate a level of nesting has ended.
     * Otherwise, we return 0 to indicate no chnage
     * @param textLine textline to analyze
     * @return the nesting level change: -1, 0, or 1
     */
    private static int checkNesting(
        final TextLine textLine
    ) {
        if (textLine._fields.size() >= 2) {
            TextField operationField = textLine._fields.get(1);
            if (operationField._subfields.size() >= 1) {
                String directive = operationField._subfields.get(0)._text;
                if (directive.equalsIgnoreCase("$END")) {
                    return -1;
                } else if (directive.equalsIgnoreCase("$PROC")) {
                    return 1;
                } else if (directive.equalsIgnoreCase("$FUNC")) {
                    return 1;
                }
            }
        }

        return 0;
    }

    /**
     * We handle $PROC slightly differently than convention.
     * The label is always assumed to be specified at the outer level, so it does not need to be externalized
     * to be accessible to the assembly which contains it.  Also, we do not (at least currently) implement the
     * $NAME, so it makes no sense to have a $PROC label accessible only within the proc.
     * @param context reference to the context in which this directive is to execute
     * @param textLine contains the basic parse into fields/subfields - we cannot drill down further, as various directives
     *                 make different usages of the fields - and $INFO even uses an extra field
     * @param labelFieldComponents LabelFieldComponents describing the label field on the line containing this directive
     */
    @Override
    public void process(
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(, context, textLine, false, 3)) {
            List<TextLine> textLines = new LinkedList<>();
            int nesting = 1;
            while (context.hasNextSourceLine() && (nesting > 0)) {
                TextLine nestedLine = context.getNextSourceLine();
                nesting += checkNesting(nestedLine);
                if (nesting > 0) {
                    textLines.add(nestedLine);
                }
            }

            if (nesting > 0) {
                Locale loc = new Locale(new LineSpecifier(0, context.sourceLineCount() + 1), 1);
                context.appendDiagnostic((new ErrorDiagnostic(loc,
                                                              "Reached end of file before end of proc")));
            }

            ProcedureValue procValue = new ProcedureValue.Builder().setValue(textLines.toArray(new TextLine[0]))
                                                                   .build();
            if (labelFieldComponents._label == null) {
                Locale loc = new Locale(textLine._lineSpecifier, 1);
                context.appendDiagnostic(new ErrorDiagnostic(loc, "Label required for $PROC directive"));
                return;
            }

            if (context.getDictionary().hasValue(labelFieldComponents._label)) {
                Locale loc = new Locale(textLine._lineSpecifier, 1);
                context.appendDiagnostic(new DuplicateDiagnostic(loc, "$PROC label duplicated"));
            } else {
                context.getDictionary().addValue(labelFieldComponents._labelLevel,
                                                 labelFieldComponents._label,
                                                 procValue);
            }
        }
    }
}
