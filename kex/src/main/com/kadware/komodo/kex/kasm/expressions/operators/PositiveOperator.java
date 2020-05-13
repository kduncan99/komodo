/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Class for positive (leading sign) operator
 */
public class PositiveOperator extends Operator {

    public PositiveOperator(Locale locale) { super(locale); }

    @Override public int getPrecedence() { return 9; }
    @Override public Type getType() { return Type.Prefix; }

    /**
     * Evaluator
     * @param assembler
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        Assembler assembler, Stack<Value> valueStack) throws ExpressionException {
        Value operand = getOperands(valueStack)[0];
        switch(operand.getType()) {
            case Integer:
            case FloatingPoint:
                valueStack.push(operand);
                break;

            default:
                postValueDiagnostic(false, context.getDiagnostics());
                throw new ExpressionException();
        }
    }
}
