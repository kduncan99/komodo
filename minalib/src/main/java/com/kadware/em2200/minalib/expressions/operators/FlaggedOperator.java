/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.Stack;

/**
 * Class for flagged (leading asterisk) operator
 */
public class FlaggedOperator extends Operator {

    /**
     * Constructor
     * <p>
     * @param locale
     */
    public FlaggedOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public int getPrecedence(
    ) {
        return 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public Type getType(
    ) {
        return Type.Prefix;
    }

    /**
     * Evaluator
     * <p>
     * @param context current contextual information one of our subclasses might need to know
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @param diagnostics where we append diagnostics if necessary
     * <p>
     * @throws ExpressionException if evaluation fails
     */
    @Override
    public void evaluate(
        final Context context,
        Stack<Value> valueStack,
        Diagnostics diagnostics
    ) throws ExpressionException {
        //  I think pretty much anything can be flagged
        Value operand = getOperands(valueStack)[0];
        try {
            valueStack.push(operand.copy(true));
        } catch (TypeException ex) {
            diagnostics.append(new ValueDiagnostic(getLocale(), "Cannot apply flag to this operand"));
            throw new ExpressionException();
        }
    }
}
