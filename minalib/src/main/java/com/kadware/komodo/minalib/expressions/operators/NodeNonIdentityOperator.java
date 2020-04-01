/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.NodeValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.dictionary.ValueType;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Class for node identity operator
 */
@SuppressWarnings("Duplicates")
public class NodeNonIdentityOperator extends RelationalOperator {

    /**
     * Constructor
     * @param locale location of operator
     */
    public NodeNonIdentityOperator(
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
    public void evaluate(
        final Context context,
        Stack<Value> valueStack
    ) throws ExpressionException {
        Value[] operands = getOperands(valueStack);

        if (operands[0].getType() != ValueType.Node) {
            postValueDiagnostic(true, context.getDiagnostics());
            throw new ExpressionException();
        }

        if (operands[1].getType() != ValueType.Node) {
            postValueDiagnostic(false, context.getDiagnostics());
            throw new ExpressionException();
        }

        NodeValue leftValue = (NodeValue)operands[0];
        NodeValue rightValue = (NodeValue)operands[1];
        int result = (leftValue != rightValue) ? 1 : 0;
        valueStack.push(new IntegerValue.Builder().setValue(result).build());
    }
}
