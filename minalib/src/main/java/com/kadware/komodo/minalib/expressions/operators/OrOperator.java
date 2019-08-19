/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.*;
import java.util.Stack;

/**
 * Class for logical OR operator
 */
@SuppressWarnings("Duplicates")
public class OrOperator extends LogicalOperator {

    /**
     * Constructor
     * @param locale location of this operator
     */
    public OrOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public int getPrecedence(
    ) {
        return 4;
    }

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
        Value[] operands = getOperands(valueStack);

        try {
            IntegerValue leftValue = operands[0].toIntegerValue(getLocale(), context.getDiagnostics());
            IntegerValue rightValue = operands[1].toIntegerValue(getLocale(), context.getDiagnostics());
            IntegerValue result = IntegerValue.or(leftValue, rightValue, getLocale(), context.getDiagnostics());
            valueStack.push(result);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
