/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.directives;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.expressions.*;

@SuppressWarnings("Duplicates")
public class INFODirective extends Directive {

    /**
     * Group 10 handler
     * @param context reference to the context in which this directive is to execute
     */
    private void handleExtendedModeLCs(
        final Context context
    ) {
        if ((_additionalOperandField == null) || (_additionalOperandField._subfields.isEmpty())) {
            context.appendDiagnostic(new ErrorDiagnostic(_operandField._locale,
                                                         "No LCs specified for $INFO group 10"));
            return;
        }

        for (TextSubfield sf : _additionalOperandField._subfields) {
            try {
                ExpressionParser p = new ExpressionParser(sf._text, sf._locale);
                Expression e = p.parse(context);

                Value v = e.evaluate(context);
                if (!(v instanceof IntegerValue)) {
                    context.appendDiagnostic(new ErrorDiagnostic(_additionalOperandField._locale,
                                                                 "Invalid value type"));
                    return;
                }

                int lcIndex = ((IntegerValue) v)._value.get().intValue();
                if ((lcIndex < 0) || (lcIndex > 063)) {
                    context.appendDiagnostic(new ErrorDiagnostic(_additionalOperandField._locale,
                                                                 "Illegal location counter index value"));
                    return;
                }

                Context.GeneratedPool gp = context.obtainPool(lcIndex);
                gp._extendedModeFlag = true;
            } catch (ExpressionException ex) {
                context.appendDiagnostic(new ErrorDiagnostic(sf._locale, "Syntax error"));
            }
        }

        if (_additionalOperandField._subfields.size() > 1) {
            context.appendDiagnostic(new ErrorDiagnostic(_additionalOperandField._locale,
                                                            "Ignoring extraneous subfields"));
        }
    }

    /**
     * Group 1 handler
     * @param context reference to the context in which this directive is to execute
     */
    private void handleProcessorModeSettings(
        final Context context
    ) {
        if ((_additionalOperandField == null) || (_additionalOperandField._subfields.isEmpty())) {
            context.appendDiagnostic(new ErrorDiagnostic(_operandField._locale,
                                                            "No value specified for $INFO group 1"));
            return;
        }

        TextSubfield sf = _additionalOperandField._subfields.get(0);
        try {
            ExpressionParser p = new ExpressionParser(sf._text, sf._locale);
            Expression e = p.parse(context);

            Value v = e.evaluate(context);
            if (!(v instanceof IntegerValue)) {
                context.appendDiagnostic(new ErrorDiagnostic(_additionalOperandField._locale, "Invalid value type"));
                return;
            }

            IntegerValue iv = (IntegerValue) v;
            long intValue = iv._value.get().longValue();
            if ((intValue < 0) || ((intValue & 07) == 07) || ((intValue & 070) == 070)) {
                context.appendDiagnostic(new ErrorDiagnostic(_additionalOperandField._locale, "Illegal value"));
                return;
            }

            if ((intValue & 03) == 03) {
                context.setQuarterWordMode();
            }
            if ((intValue & 05) == 05) {
                context.setThirdWordMode();
            }
            if ((intValue & 030) == 030) {
                context.setArithmeticFaultCompatibilityMode();
            }
            if ((intValue & 050) == 050) {
                context.setArithmeticFaultNonInterruptMode();
            }
        } catch (ExpressionException ex) {
            context.appendDiagnostic(new ErrorDiagnostic(sf._locale, "Syntax error"));
        }

        if (_additionalOperandField._subfields.size() > 1) {
            context.appendDiagnostic(new ErrorDiagnostic(_additionalOperandField._locale, "Ignoring extraneous subfields"));
        }
    }

    /**
     * Main routine
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
        if (extractFields(context, textLine, true, 4)) {
            Locale opLoc = _operandField._subfields.get(0)._locale;
            String opText = _operandField._subfields.get(0)._text;
            try {
                ExpressionParser p = new ExpressionParser(opText, opLoc);
                Expression e = p.parse(context);
                Value v = e.evaluate(context);
                if ((!(v instanceof IntegerValue)) || (((IntegerValue) v).hasUndefinedReferences())) {
                    context.appendDiagnostic(new ValueDiagnostic(opLoc,
                                                                 "Invalid value type for $INFO group cateogry"));
                } else {
                    switch (((IntegerValue) v)._value.get().intValue()) {
                        case 1:     //  Processor Mode Settings
                            handleProcessorModeSettings(context);
                            break;

                        case 10:    //  Extended Mode Location Counter
                            handleExtendedModeLCs(context);
                            break;

                        case 2:     //  Common Block
                        case 3:     //  Minimum D-Bank Specification
                        case 4:     //  Blank Common Block
                        case 5:     //  External Reference Definition
                        case 6:     //  Entry-Point Definition
                        case 7:     //  Even Starting Address
                        case 8:     //  Static Diagnostic Information
                        case 9:     //  Read-Only Location Counters
                        case 11:    //  Void Bank
                        case 12:    //  Library Search File
                        default:
                            context.appendDiagnostic(new ErrorDiagnostic(opLoc,
                                                                            "Unknown or unimplemented $INFO group"));
                    }
                }
            } catch (ExpressionException ex) {
                context.appendDiagnostic(new ErrorDiagnostic(opLoc, "Syntax error"));
            }

            if (_operandField._subfields.size() > 1) {
                context.appendDiagnostic(new ErrorDiagnostic(_operandField._subfields.get(1)._locale,
                                                                "Extraneous subfields ignored"));
            }
        }
    }
}
