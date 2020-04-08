/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Class for logical OR operator
 */
@SuppressWarnings("Duplicates")
public class OrOperator extends LogicalOperator {

    public OrOperator(Locale locale) { super(locale); }
    @Override public int getPrecedence() { return 4; }
    @Override public Type getType() { return Type.Infix; }

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
        Value[] operands = getOperands(valueStack, context);
        IntegerValue leftValue = (IntegerValue) operands[0];
        IntegerValue rightValue = (IntegerValue) operands[1];
        IntegerValue result = IntegerValue.or(leftValue, rightValue, _locale, context.getDiagnostics());
        valueStack.push(result);
    }
}
