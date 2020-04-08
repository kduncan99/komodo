/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.FormDiagnostic;
import com.kadware.komodo.minalib.diagnostics.RelocationDiagnostic;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.*;
import java.math.BigInteger;
import java.util.Stack;

/**
 * Class for bit-shift operator
 */
public class ShiftOperator extends ArithmeticOperator {

    public ShiftOperator(Locale locale) { super(locale); }

    @Override public final int getPrecedence() { return 7; }

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

            //  Forms are not allowed for either operand
            if ((iopLeft._form != null) || (iopRight._form != null)) {
                context.appendDiagnostic(new FormDiagnostic(_locale));
            }

            //  Undefined references not allowed for either operand
            if (iopLeft.hasUndefinedReferences() || iopRight.hasUndefinedReferences()) {
                context.appendDiagnostic(new RelocationDiagnostic(_locale));
            }

            BigInteger result = iopLeft._value.get();
            int count = iopRight._value.get().intValue();
            if (count < 0) {
                result = result.shiftRight(-count);
            } else {
                result = result.shiftLeft(count);
            }

            valueStack.push(new IntegerValue.Builder().setValue(new DoubleWord36(result)).build());
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
