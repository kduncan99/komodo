/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.Stack;

/**
 * Class for bit-shift operator
 */
public class ShiftOperator extends ArithmeticOperator {

    /**
     * Constructor
     * @param locale location of operator
     */
    public ShiftOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public final int getPrecedence(
    ) {
        return 7;
    }

    /**
     * Evaluator
     * We do *NOT* flag T's on left shifts out of MSBit, contra MASM.
     * @param context current contextual information one of our subclasses might need to know
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        final Context context,
        Stack<Value> valueStack
    ) throws ExpressionException {
        try {
            Value[] operands = getTransformedOperands(valueStack, false, context.getDiagnostics());

            IntegerValue iopLeft = (IntegerValue)operands[0];
            IntegerValue iopRight = (IntegerValue)operands[1];

            //  Undefined references not allowed for the right-hand operand
            if (iopRight._undefinedReferences.length != 0) {
                context.appendDiagnostic(new RelocationDiagnostic(getLocale()));
            }

            long result = iopLeft._value;
            long count = iopRight._value;
            if (count < 0) {
                result >>= (-count);
            } else {
                result <<= count;
            }

            valueStack.push(new IntegerValue(iopLeft._flagged, result, iopLeft._undefinedReferences));
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
