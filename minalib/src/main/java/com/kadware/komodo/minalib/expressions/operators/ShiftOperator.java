/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.FormDiagnostic;
import com.kadware.komodo.minalib.diagnostics.RelocationDiagnostic;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.*;
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

            //  Forms other than 1 36-bit form are not allowed for either operand
            if ((iopLeft.getFieldCount() > 1) || (iopRight.getFieldCount() > 1)) {
                context.appendDiagnostic(new FormDiagnostic(getLocale()));
            }

            //  Undefined references not allowed for either operand
            if (iopLeft.hasUndefinedReferences() || iopRight.hasUndefinedReferences()) {
                context.appendDiagnostic(new RelocationDiagnostic(getLocale()));
            }

            long result = iopLeft.getIntrinsicValue();
            long count = iopRight.getIntrinsicValue();
            if (count < 0) {
                result >>= (-count);
            } else {
                result <<= count;
            }

            valueStack.push(new IntegerValue(result));
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
