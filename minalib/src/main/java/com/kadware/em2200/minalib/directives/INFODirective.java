/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.ExpressionException;
import com.kadware.em2200.minalib.expressions.*;

@SuppressWarnings("Duplicates")
public class INFODirective extends Directive {

    /**
     * Group 10 handler
     * @param context reference to the context in which this directive is to execute
     * @param diagnostics where diagnostics should be posted if necessary
     */
    private void handleExtendedModeLCs(
        final Context context,
        final Diagnostics diagnostics
    ) {
        if ((_additionalOperandField == null) || (_additionalOperandField._subfields.isEmpty())) {
            diagnostics.append(new ErrorDiagnostic(_operandField._locale,
                                                   "No LCs specified for $INFO group 10"));
            return;
        }

        for (TextSubfield sf : _additionalOperandField._subfields) {
            try {
                ExpressionParser p = new ExpressionParser(sf._text, sf._locale);
                Expression e = p.parse(context, diagnostics);

                Value v = e.evaluate(context, diagnostics);
                if (!(v instanceof IntegerValue)) {
                    diagnostics.append(new ErrorDiagnostic(_additionalOperandField._locale,
                                                           "Invalid value type"));
                    return;
                }

                int lcIndex = (int) ((IntegerValue) v)._value;
                if ((lcIndex < 0) || (lcIndex > 063)) {
                    diagnostics.append(new ErrorDiagnostic(_additionalOperandField._locale,
                                                           "Illegal location counter index value"));
                    return;
                }

                Context.GeneratedPool gp = context._generatedPools.get(lcIndex);
                if (gp == null) {
                    gp = new Context.GeneratedPool();
                    context._generatedPools.put(lcIndex, gp);
                }
                gp._extendedModeFlag = true;
            } catch (ExpressionException ex) {
                diagnostics.append(new ErrorDiagnostic(sf._locale, "Syntax error"));
            }
        }

        if (_additionalOperandField._subfields.size() > 1) {
            diagnostics.append(new ErrorDiagnostic(_additionalOperandField._locale,
                                                   "Ignoring extraneous subfields"));
        }
    }

    /**
     * Group 1 handler
     * @param context reference to the context in which this directive is to execute
     * @param diagnostics where diagnostics should be posted if necessary
     */
    private void handleProcessorModeSettings(
        final Context context,
        final Diagnostics diagnostics
    ) {
        if ((_additionalOperandField == null) || (_additionalOperandField._subfields.isEmpty())) {
            diagnostics.append(new ErrorDiagnostic(_operandField._locale,
                                                   "No value specified for $INFO group 1"));
            return;
        }

        TextSubfield sf = _additionalOperandField._subfields.get(0);
        try {
            ExpressionParser p = new ExpressionParser(sf._text, sf._locale);
            Expression e = p.parse(context, diagnostics);

            Value v = e.evaluate(context, diagnostics);
            if (!(v instanceof IntegerValue)) {
                diagnostics.append(new ErrorDiagnostic(_additionalOperandField._locale,
                                                       "Invalid value type"));
                return;
            }

            IntegerValue iv = (IntegerValue) v;
            if ((iv._value < 0) || ((iv._value & 07) == 07) || ((iv._value & 070) == 070)) {
                diagnostics.append(new ErrorDiagnostic(_additionalOperandField._locale,
                                                       "Illegal value"));
                return;
            }

            context.setQuarterWordMode((iv._value & 03) == 03);
            context.setThirdWordMode((iv._value & 05) == 05);
            context.setArithmeticFaultCompatibilityMode((iv._value & 030) == 030);
            context.setArithmeticFaultNonInterruptMode((iv._value & 050) == 050);
        } catch (ExpressionException ex) {
            diagnostics.append(new ErrorDiagnostic(sf._locale, "Syntax error"));
        }

        if (_additionalOperandField._subfields.size() > 1) {
            diagnostics.append(new ErrorDiagnostic(_additionalOperandField._locale,
                                                   "Ignoring extraneous subfields"));
        }
    }

    /**
     * Main routine
     * @param context reference to the context in which this directive is to execute
     * @param textLine contains the basic parse into fields/subfields - we cannot drill down further, as various directives
     *                 make different usages of the fields - and $INFO even uses an extra field
     * @param labelFieldComponents LabelFieldComponents describing the label field on the line containing this directive
     * @param diagnostics where diagnostics should be posted if necessary
     */
    @Override
    public void process(
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents,
            final Diagnostics diagnostics
    ) {
        if (extractFields(textLine, true, 4, diagnostics)) {
            Locale opLoc = _operandField._subfields.get(0)._locale;
            String opText = _operandField._subfields.get(0)._text;
            try {
                ExpressionParser p = new ExpressionParser(opText, opLoc);
                Expression e = p.parse(context, diagnostics);
                Value v = e.evaluate(context, diagnostics);
                if ((!(v instanceof IntegerValue)) || (((IntegerValue) v)._undefinedReferences.length > 0)) {
                    diagnostics.append(new ValueDiagnostic(opLoc, "Invalid value type for $INFO group cateogry"));
                } else {
                    switch ((int) ((IntegerValue) v)._value) {
                        case 1:     //  Processor Mode Settings
                            handleProcessorModeSettings(context, diagnostics);
                            break;

                        case 10:    //  Extended Mode Location Counter
                            handleExtendedModeLCs(context, diagnostics);
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
                            diagnostics.append(new ErrorDiagnostic(opLoc, "Unknown or unimplemented $INFO group"));
                    }
                }
            } catch (ExpressionException ex) {
                diagnostics.append(new ErrorDiagnostic(opLoc, "Syntax error"));
            }

            if (_operandField._subfields.size() > 1) {
                diagnostics.append(new ErrorDiagnostic(_operandField._subfields.get(1)._locale,
                                                       "Extraneous subfields ignored"));
            }
        }
    }
}
