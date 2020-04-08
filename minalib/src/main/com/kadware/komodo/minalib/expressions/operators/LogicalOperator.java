/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Base class for infix logical operators
 */
public abstract class LogicalOperator extends Operator {

    LogicalOperator(Locale locale) { super(locale); }

    /**
     * Evaluator
     * @param context current contextual information one of our subclasses might need to know
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public abstract void evaluate(
        final Context context,
        Stack<Value> valueStack
    ) throws ExpressionException;

    /**
     * Retrieves the type of this operator
     * @return value
     */
    @Override
    public abstract Type getType();

    /**
     * Extra sauce on the generic routine ...
     * All logical operators require integer values
     */
    protected Value[] getOperands(
        final Stack<Value> valueStack,
        final Context context
    ) throws ExpressionException {
        Value[] result = getOperands(valueStack);
        boolean error = false;
        for (int vx = 0; vx < result.length; ++vx) {
            if (!(result[vx] instanceof IntegerValue)) {
                postValueDiagnostic((result.length > 1) && (vx == 0), context.getDiagnostics());
                error = true;
            }
        }

        if (error) {
            throw new ExpressionException();
        }

        return result;
    }
}
