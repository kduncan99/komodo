/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Base class for infix logical operators
 */
public abstract class LogicalOperator extends Operator {

    LogicalOperator(Locale locale) { super(locale); }

    /**
     * Evaluator
     * @param assembler context
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public abstract void evaluate(
        final Assembler assembler,
        final Stack<Value> valueStack
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
        final Assembler assembler
    ) throws ExpressionException {
        Value[] result = getOperands(valueStack);
        boolean error = false;
        for (int vx = 0; vx < result.length; ++vx) {
            if (!(result[vx] instanceof IntegerValue)) {
                postValueDiagnostic((result.length > 1) && (vx == 0), assembler.getDiagnostics());
                error = true;
            }
        }

        if (error) {
            throw new ExpressionException();
        }

        return result;
    }
}
