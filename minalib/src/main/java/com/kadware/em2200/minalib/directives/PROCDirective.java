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

    private static boolean checkForEnd(
        final TextLine textLine
    ) {
        return ((textLine._fields.size() >= 2)
                && (textLine._fields.get(1) != null)
                && (textLine._fields.get(1)._text.equalsIgnoreCase("$END")));
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
            boolean done = false;
            while (!done) {
                if (!context.hasNextSourceLine()) {
                    Locale loc = new Locale(context.sourceLineCount() + 1, 1);
                    context._diagnostics.append((new ErrorDiagnostic(loc,
                                                                     "Reached end of file before end of proc")));
                    done = true;
                } else {
                    TextLine procLine = context.getNextSourceLine();
                    if (checkForEnd(procLine)) {
                        done = true;
                    } else {
                        textLines.add(procLine);
                    }
                }
            }

            ProcedureValue procValue = new ProcedureValue(false, textLines.toArray(new TextLine[0]));
            if (labelFieldComponents._label == null) {
                Locale loc = new Locale(procLineNumber, 1);
                context._diagnostics.append(new ErrorDiagnostic(loc, "Label required for $PROC directive"));
                return;
            }

            if (context._dictionary.hasValue(labelFieldComponents._label)) {
                Locale loc = new Locale(procLineNumber, 1);
                context._diagnostics.append(new DuplicateDiagnostic(loc, "$PROC label duplicated"));
            } else {
                context._dictionary.addValue(labelFieldComponents._labelLevel,
                                             labelFieldComponents._label,
                                             procValue);
            }
        }
    }
}
