/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Class for node identity operator
 */
public class NodeIdentityOperator extends RelationalOperator {

    /**
     * Constructor
     * <p>
     * @param locale
     */
    public NodeIdentityOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Evaluator
     * <p>
     * @param context current contextual information one of our subclasses might need to know
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @param diagnostics where we append diagnostics if necessary
     * <p>
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        final Context context,
        Stack<Value> valueStack,
        Diagnostics diagnostics
    ) throws ExpressionException {
        Value[] operands = getOperands(valueStack);

        if (operands[0].getType() != ValueType.Node) {
            postValueDiagnostic(true, diagnostics);
            throw new ExpressionException();
        }

        if (operands[1].getType() != ValueType.Node) {
            postValueDiagnostic(false, diagnostics);
            throw new ExpressionException();
        }

        NodeValue leftValue = (NodeValue)operands[0];
        NodeValue rightValue = (NodeValue)operands[1];
        int result = (leftValue == rightValue) ? 1 : 0;
        valueStack.push(new IntegerValue.Builder().setValue(result)
                                                  .build());
    }
}
