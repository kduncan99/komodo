/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.diagnostics.*;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.*;

@SuppressWarnings("Duplicates")
public class INFODirective extends Directive {

    /**
     * Group 10 handler
     * @param assembler reference to the assembler in which this directive is to execute
     */
    private void handleExtendedModeLCs(
        final Assembler assembler
    ) {
        if ((_additionalOperandField == null) || (_additionalOperandField._subfields.isEmpty())) {
            assembler.appendDiagnostic(new ErrorDiagnostic(_operandField._locale,
                                                           "No LCs specified for $INFO group 10"));
            return;
        }

        for (TextSubfield sf : _additionalOperandField._subfields) {
            try {
                ExpressionParser p = new ExpressionParser(sf._text, sf._locale);
                Expression e = p.parse(assembler);

                Value v = e.evaluate(assembler);
                if (!(v instanceof IntegerValue)) {
                    assembler.appendDiagnostic(new ErrorDiagnostic(_additionalOperandField._locale,
                                                                   "Invalid value type"));
                    return;
                }

                int lcIndex = ((IntegerValue) v)._value.get().intValue();
                if ((lcIndex < 0) || (lcIndex > 063)) {
                    assembler.appendDiagnostic(new ErrorDiagnostic(_additionalOperandField._locale,
                                                                   "Illegal location counter index value"));
                    return;
                }

                GeneratedPool gp = assembler.obtainPool(lcIndex);
                gp.setExtendedModeFlag(true);
            } catch (ExpressionException ex) {
                assembler.appendDiagnostic(new ErrorDiagnostic(sf._locale, "Syntax error"));
            }
        }

//        if (_additionalOperandField._subfields.size() > 1) {
//            assembler.appendDiagnostic(new ErrorDiagnostic(_additionalOperandField._locale,
//                                                           "Ignoring extraneous subfields"));
//        }
    }

    /**
     * Group 1 handler
     * @param assembler reference to the assembler in which this directive is to execute
     */
    private void handleProcessorModeSettings(
        final Assembler assembler
    ) {
        if ((_additionalOperandField == null) || (_additionalOperandField._subfields.isEmpty())) {
            assembler.appendDiagnostic(new ErrorDiagnostic(_operandField._locale,
                                                            "No value specified for $INFO group 1"));
            return;
        }

        TextSubfield sf = _additionalOperandField._subfields.get(0);
        try {
            ExpressionParser p = new ExpressionParser(sf._text, sf._locale);
            Expression e = p.parse(assembler);

            Value v = e.evaluate(assembler);
            if (!(v instanceof IntegerValue)) {
                assembler.appendDiagnostic(new ErrorDiagnostic(_additionalOperandField._locale, "Invalid value type"));
                return;
            }

            IntegerValue iv = (IntegerValue) v;
            long intValue = iv._value.get().longValue();
            if ((intValue < 0) || ((intValue & 07) == 07) || ((intValue & 070) == 070)) {
                assembler.appendDiagnostic(new ErrorDiagnostic(_additionalOperandField._locale, "Illegal value"));
                return;
            }

            if ((intValue & 03) == 03) {
                if (assembler.getThirdWordMode()) {
                    assembler.appendDiagnostic(new WarningDiagnostic(_additionalOperandField._locale,
                                                                     "Conflicting third/quarter word modes"));
                }
                assembler.setQuarterWordMode();
            }
            if ((intValue & 05) == 05) {
                if (assembler.getQuarterWordMode()) {
                    assembler.appendDiagnostic(new WarningDiagnostic(_additionalOperandField._locale,
                                                                     "Conflicting third/quarter word modes"));
                }
                assembler.setThirdWordMode();
            }
            if ((intValue & 030) == 030) {
                if (assembler.getArithmeticFaultNonInterruptMode()) {
                    assembler.appendDiagnostic(new WarningDiagnostic(_additionalOperandField._locale,
                                                                     "Conflicting AFCM modes"));
                }
                assembler.setArithmeticFaultCompatibilityMode();
            }
            if ((intValue & 050) == 050) {
                if (assembler.getArithmeticFaultCompatibilityMode()) {
                    assembler.appendDiagnostic(new WarningDiagnostic(_additionalOperandField._locale,
                                                                     "Conflicting AFCM modes"));
                }
                assembler.setArithmeticFaultNonInterruptMode();
            }
        } catch (ExpressionException ex) {
            assembler.appendDiagnostic(new ErrorDiagnostic(sf._locale, "Syntax error"));
        }

        if (_additionalOperandField._subfields.size() > 1) {
            assembler.appendDiagnostic(new ErrorDiagnostic(_additionalOperandField._locale, "Ignoring extraneous subfields"));
        }
    }

    /**
     * Main routine
     * @param assembler reference to the assembler in which this directive is to execute
     * @param textLine contains the basic parse into fields/subfields - we cannot drill down further, as various directives
     *                 make different usages of the fields - and $INFO even uses an extra field
     * @param labelFieldComponents LabelFieldComponents describing the label field on the line containing this directive
     */
    @Override
    public void process(
        final Assembler assembler,
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(assembler, textLine, true, 4)) {
            Locale opLoc = _operandField._subfields.get(0)._locale;
            String opText = _operandField._subfields.get(0)._text;
            try {
                ExpressionParser p = new ExpressionParser(opText, opLoc);
                Expression e = p.parse(assembler);
                Value v = e.evaluate(assembler);
                if ((!(v instanceof IntegerValue)) || (((IntegerValue) v).hasUndefinedReferences())) {
                    assembler.appendDiagnostic(new ValueDiagnostic(opLoc,
                                                                 "Invalid value type for $INFO group cateogry"));
                } else {
                    switch (((IntegerValue) v)._value.get().intValue()) {
                        case 1 -> handleProcessorModeSettings(assembler);
                        case 10 -> handleExtendedModeLCs(assembler);
                        default -> assembler.appendDiagnostic(new ErrorDiagnostic(opLoc,
                                                                                  "Unknown or unimplemented $INFO group"));
                    }
                }
            } catch (ExpressionException ex) {
                assembler.appendDiagnostic(new ErrorDiagnostic(opLoc, "Syntax error"));
            }

            if (_operandField._subfields.size() > 1) {
                assembler.appendDiagnostic(new ErrorDiagnostic(_operandField._subfields.get(1)._locale,
                                                                "Extraneous subfields ignored"));
            }
        }
    }
}
