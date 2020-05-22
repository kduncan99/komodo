/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.NodeValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.dictionary.ValueType;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Class for node identity operator
 */
@SuppressWarnings("Duplicates")
public class NodeIdentityOperator extends RelationalOperator {

    /**
     * Constructor
     * @param locale location of operator
     */
    public NodeIdentityOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Evaluator
     * @param assembler context
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        final Assembler assembler,
        final Stack<Value> valueStack
    ) throws ExpressionException {
        Value[] operands = getOperands(valueStack);

        if (operands[0].getType() != ValueType.Node) {
            postValueDiagnostic(true, assembler.getDiagnostics());
            throw new ExpressionException();
        }

        if (operands[1].getType() != ValueType.Node) {
            postValueDiagnostic(false, assembler.getDiagnostics());
            throw new ExpressionException();
        }

        NodeValue leftValue = (NodeValue)operands[0];
        NodeValue rightValue = (NodeValue)operands[1];
        int result = (leftValue == rightValue) ? 1 : 0;
        valueStack.push(new IntegerValue.Builder().setValue(result).build());
    }
}
