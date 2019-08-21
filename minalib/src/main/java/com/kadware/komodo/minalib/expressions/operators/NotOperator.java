/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.diagnostics.ValueDiagnostic;
import com.kadware.komodo.minalib.exceptions.ExpressionException;

import java.util.Stack;

/**
 * Class for negation operator
 */
public class NotOperator extends Operator {

    /**
     * Constructor
     * @param locale location of operator
     */
    public NotOperator(
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
        return 1;
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public Type getType(
    ) {
        return Type.Prefix;
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
        Value operand = getOperands(valueStack)[0];
        if (operand.getType() == ValueType.Integer) {
            IntegerValue ioperand = (IntegerValue) operand;
            IntegerValue iresult = ioperand.negate();
            valueStack.push(iresult);
        } else {
            postValueDiagnostic(false, context.getDiagnostics());
            throw new ExpressionException();
        }
    }
}
