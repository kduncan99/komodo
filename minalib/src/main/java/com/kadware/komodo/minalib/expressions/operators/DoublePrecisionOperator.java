/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.dictionary.ValuePrecision;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.exceptions.TypeException;
import java.util.Stack;

/**
 * Class for single precision operator
 */
public class DoublePrecisionOperator extends Operator {

    public DoublePrecisionOperator(Locale locale) { super(locale); }
    @Override public int getPrecedence() { return 10; }
    @Override public Type getType() { return Type.Postfix; }

    /**
     * Evaluator
     * @param context current contextual information one of our subclasses might need to know
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if evaluation fails
     */
    @Override
    public void evaluate(
        final Context context,
        Stack<Value> valueStack
    ) throws ExpressionException {
        Value operand = getOperands(valueStack)[0];
        try {
            valueStack.push(operand.copy(ValuePrecision.Double));
        } catch (TypeException ex) {
            postValueDiagnostic(false, context.getDiagnostics());
            throw new ExpressionException();
        }
    }
}
