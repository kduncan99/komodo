/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.CodeMode;
import com.kadware.komodo.kex.kasm.Form;
import com.kadware.komodo.kex.kasm.LabelFieldComponents;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.TextLine;
import com.kadware.komodo.kex.kasm.TextSubfield;
import com.kadware.komodo.kex.kasm.UndefinedReference;
import com.kadware.komodo.kex.kasm.diagnostics.TruncationDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.EqufValue;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;
import com.kadware.komodo.kex.kasm.expressions.ExpressionParser;
import java.math.BigInteger;
import java.util.Arrays;

@SuppressWarnings("Duplicates")
public class EQUFDirective extends Directive {

    /**
     * Parses the operands into expressions, then evaluates them.
     * Once done, we attempt to fit them into an appropriate form, and store the whole thing
     * as an EqufValue with the various bit fields set according to the expression evaluations.
     * In basic mode, the format of the operands is u,x,j and they are placed into an I$ form.
     * In extended mode, the format is u,x,j,b and they are placed into an EI$ form.
     */

    private final FieldDescriptor B_DESCRIPTOR = new FieldDescriptor(20, 4);
    private final FieldDescriptor J_DESCRIPTOR = new FieldDescriptor(6, 4);
    private final FieldDescriptor U_DESCRIPTOR_BASIC = new FieldDescriptor(20,16);
    private final FieldDescriptor U_DESCRIPTOR_EXTENDED = new FieldDescriptor(24, 12);
    private final FieldDescriptor X_DESCRIPTOR = new FieldDescriptor(14, 4);

    private IntegerValue integrate(
        final Assembler assembler,
        final Locale locale,
        final IntegerValue baseValue,
        final FieldDescriptor fieldDescriptor,
        final IntegerValue fieldValue
    ) {
        BigInteger fieldMask = BigInteger.valueOf((1L << fieldDescriptor._fieldSize) - 1);
        BigInteger maskedInt = fieldValue._value.get().and(fieldMask);
        if (!maskedInt.equals(fieldValue._value.get())) {
            assembler.appendDiagnostic(new TruncationDiagnostic(locale, "Value too large for field"));
        }

        long shiftedInt = maskedInt.longValue() << (36 - fieldDescriptor._startingBit - fieldDescriptor._fieldSize);
        BigInteger finalInt = baseValue._value.get().or(BigInteger.valueOf(shiftedInt));

        UndefinedReference[] finalRefs = new UndefinedReference[baseValue._references.length + fieldValue._references.length];
        int finalrx = 0;
        for (int brx = 0; brx < baseValue._references.length; ++brx, ++finalrx) {
            finalRefs[finalrx] = baseValue._references[brx];
        }
        for (int frx = 0; frx < fieldValue._references.length; ++frx, ++finalrx) {
            finalRefs[finalrx] = fieldValue._references[frx].copy(fieldDescriptor);
        }

        return new IntegerValue.Builder().setValue(new DoubleWord36(finalInt))
                                         .setReferences(finalRefs)
                                         .build();
    }

    @Override
    public void process(
        final Assembler assembler,
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(assembler, textLine, true, 3)) {
            if (labelFieldComponents._label == null) {
                Locale loc = new Locale(textLine._lineSpecifier, 1);
                assembler.appendDiagnostic(new ErrorDiagnostic(loc, "Label required for $EQUF directive"));
                return;
            }

            if (_operandField._subfields.isEmpty()) {
                assembler.appendDiagnostic(new ErrorDiagnostic(_operationField._locale, "Syntax error"));
                return;
            }

            boolean basicMode = assembler.getCodeMode() == CodeMode.Basic;
            int maxSubfields = basicMode ? 3 : 4;
            IntegerValue[] fieldValues = new IntegerValue[maxSubfields];
            Arrays.fill(fieldValues, IntegerValue.POSITIVE_ZERO);

            for (int opx = 0; opx < maxSubfields; ++opx) {
                TextSubfield sf = _operandField._subfields.get(opx);
                String expText = sf._text;
                Locale expLocale = sf._locale;

                if (!expText.isEmpty()) {
                    try {
                        boolean good = false;
                        ExpressionParser p = new ExpressionParser(expText, expLocale);
                        Expression e = p.parse(assembler);
                        if (e != null) {
                            Value v = e.evaluate(assembler);
                            if (v instanceof IntegerValue) {
                                fieldValues[opx] = (IntegerValue) v;
                                good = true;
                            }
                        }

                        if (!good) {
                            throw new ExpressionException();
                        }
                    } catch (ExpressionException ex) {
                        assembler.appendDiagnostic(new ErrorDiagnostic(expLocale, "Syntax error"));
                    }
                }
            }

            IntegerValue uValue = fieldValues[0];
            IntegerValue xValue = fieldValues[1];
            IntegerValue jValue = fieldValues[2];
            IntegerValue bValue = basicMode ? IntegerValue.POSITIVE_ZERO : fieldValues[3];
            boolean uFlagged = uValue._flagged;
            boolean xFlagged = xValue._flagged;

            long base = (uFlagged ? 0_200000 : 0) | (xFlagged ? 0_400000 : 0);
            IntegerValue workingValue = new IntegerValue.Builder().setValue(base).build();

            if (!uValue.equals(IntegerValue.POSITIVE_ZERO)) {
                workingValue = integrate(assembler,
                                         _operandField._subfields.get(0)._locale,
                                         workingValue,
                                         basicMode ? U_DESCRIPTOR_BASIC : U_DESCRIPTOR_EXTENDED,
                                         uValue);
            }

            if (!xValue.equals(IntegerValue.POSITIVE_ZERO)) {
                workingValue = integrate(assembler,
                                         _operandField._subfields.get(1)._locale,
                                         workingValue,
                                         X_DESCRIPTOR,
                                         xValue);
            }

            if (!jValue.equals(IntegerValue.POSITIVE_ZERO)) {
                workingValue = integrate(assembler,
                                         _operandField._subfields.get(2)._locale,
                                         workingValue,
                                         J_DESCRIPTOR,
                                         jValue);
            }

            if (!bValue.equals(IntegerValue.POSITIVE_ZERO)) {
                workingValue = integrate(assembler,
                                         _operandField._subfields.get(3)._locale,
                                         workingValue,
                                         B_DESCRIPTOR,
                                         bValue);
            }

            Form form = basicMode ? Form.I$Form : Form.EI$Form;
            IntegerValue finalValue = workingValue.copy(form);
            EqufValue equfValue = new EqufValue.Builder().setValue(finalValue).build();
            assembler.getDictionary().addValue(labelFieldComponents._labelLevel,
                                             labelFieldComponents._label,
                                             equfValue);
        }
    }
}
