/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Class for positive (leading sign) operator
 */
public class PositiveOperator extends Operator {

    public PositiveOperator(Locale locale) { super(locale); }

    @Override public int getPrecedence() { return 9; }
    @Override public Type getType() { return Type.Prefix; }

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
        Value operand = getOperands(valueStack)[0];
        switch(operand.getType()) {
            case Integer:
            case FloatingPoint:
                valueStack.push(operand);
                break;

            default:
                postValueDiagnostic(false, context.getDiagnostics());
                throw new ExpressionException();
        }
    }
}
