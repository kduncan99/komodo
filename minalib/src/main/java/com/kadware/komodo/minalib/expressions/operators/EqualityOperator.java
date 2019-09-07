/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
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
 * Class for equality operator
 */
public class EqualityOperator extends RelationalOperator {

    public EqualityOperator(Locale locale) { super(locale); }

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
            Value[] operands = getTransformedOperands(valueStack, context.getDiagnostics());
            int result = (operands[0].equals(operands[1])) ? 1 : 0;
            valueStack.push(new IntegerValue.Builder().setValue(result).build());
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
