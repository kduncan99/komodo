/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.exceptions.TypeException;
import java.util.Stack;

/**
 * Class for remainder operator
 */
@SuppressWarnings("Duplicates")
public class DivisionRemainderOperator extends ArithmeticOperator {

    public DivisionRemainderOperator(Locale locale) { super(locale); }
    @Override public final int getPrecedence() { return 7; }

    /**
     * Evaluator
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
            IntegerValue iopLeft = (IntegerValue) operands[0];
            IntegerValue iopRight = (IntegerValue) operands[1];
            IntegerValue.DivisionResult dres = IntegerValue.divide(iopLeft, iopRight, _locale, context.getDiagnostics());
            valueStack.push(dres._remainder);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
