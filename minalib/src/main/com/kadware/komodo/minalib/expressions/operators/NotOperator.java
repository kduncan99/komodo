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
 * Class for negation operator
 */
public class NotOperator extends LogicalOperator {

    public NotOperator(Locale locale) { super(locale); }
    @Override public int getPrecedence() { return 1; }
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
        Value[] operands = getOperands(valueStack, context);
        IntegerValue ioperand = (IntegerValue) operands[0];
        IntegerValue iresult = ioperand.negate();
        valueStack.push(iresult);
    }
}
