/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Base class for infix logical operators
 */
public abstract class LogicalOperator extends Operator {

    /**
     * Constructor
     * @param locale indicates the line and column where this operator was specified
     */
    LogicalOperator(
        final Locale locale
    ) {
        super(locale);
    }

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
    public final Type getType(
    ) {
        return Type.Infix;
    }
}
