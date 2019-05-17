/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
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

    @Override
    public void process(
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(context, textLine, true, 3)) {
            int procLineNumber = textLine._lineNumber;
            List<TextLine> textLines = new LinkedList<>();
            int nesting = 1;
            while (context.hasNextSourceLine()) {
                TextLine nestedLine = context.getNextSourceLine();
                nesting += checkNesting(nestedLine);
                if (nesting > 0) {
                    textLines.add(nestedLine);
                }
            }

            if (nesting > 0) {
                Locale loc = new Locale(context.sourceLineCount() + 1, 1);
                context.appendDiagnostic((new ErrorDiagnostic(loc,
                                                              "Reached end of file before end of proc")));
            }

            ProcedureValue procValue = new ProcedureValue(false, textLines.toArray(new TextLine[0]));
            if (labelFieldComponents._label == null) {
                Locale loc = new Locale(procLineNumber, 1);
                context.appendDiagnostic(new ErrorDiagnostic(loc, "Label required for $PROC directive"));
                return;
            }

            if (context.getDictionary().hasValue(labelFieldComponents._label)) {
                Locale loc = new Locale(procLineNumber, 1);
                context.appendDiagnostic(new DuplicateDiagnostic(loc, "$PROC label duplicated"));
            } else {
                context.getDictionary().addValue(labelFieldComponents._labelLevel,
                                                 labelFieldComponents._label,
                                                 procValue);
            }
        }
    }
}
