/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;

public abstract class Directive {

    TextField _operationField;
    TextField _operandField;
    TextField _additionalOperandField;      //  primarily for $INFO

    /**
     * Extracts the fields from the TextLine object, and checks for extranous fields
     * @param context context of the assembler
     * @param textLine object from which we extract the fields
     * @param requiresOperand true if the operand field is required
     * @param maxFields maximum number of fields we allow
     * @return true if we found no errors which would preclude continuation of directive processing, else false
     */
    boolean extractFields(
        final Context context,
        final TextLine textLine,
        final boolean requiresOperand,
        final int maxFields
    ) {
        _operationField = textLine._fields.size() > 1 ? textLine._fields.get(1) : null;
        _operandField = textLine._fields.size() > 2 ? textLine._fields.get(2) : null;
        _additionalOperandField = textLine._fields.size() > 3 ? textLine._fields.get(3) : null;

        if (requiresOperand) {
        if ((_operandField == null) || _operandField._subfields.isEmpty()) {
                context._diagnostics.append(new ErrorDiagnostic(new Locale(textLine._lineNumber, 1),
                                                                "Directive requires an operand field"));
                return false;
            }

            if (_operationField._subfields.size() > 1) {
                context._diagnostics.append(new ErrorDiagnostic(_operationField._subfields.get(1)._locale,
                                                                 "Extranous subfields on operation field ignored"));
            }
        }

        if (textLine._fields.size() > maxFields) {
            context._diagnostics.append(new ErrorDiagnostic(textLine._fields.get(maxFields)._locale,
                                                             "Extraneous fields in directive are ignored"));
        }

        return true;
    }

    /**
     * Processes this directive
     * @param context reference to the context in which this directive is to execute
     * @param textLine contains the basic parse into fields/subfields - we cannot drill down further, as various directives
     *                 make different usages of the fields - and $INFO even uses an extra field
     * @param labelFieldComponents LabelFieldComponents describing the label field on the line containing this directive
     */
    public abstract void process(
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents
    );
}
