/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.exceptions.TypeException;
import java.util.Stack;

/**
 * Class for flagged (leading asterisk) operator
 */
public class FlaggedOperator extends Operator {

    public FlaggedOperator(Locale locale) { super(locale); }
    @Override public int getPrecedence() { return 0; }
    @Override public Type getType() { return Type.Prefix; }

    /**
     * Evaluator
     * @param assembler context
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if evaluation fails
     */
    @Override
    public void evaluate(
        final Assembler assembler,
        final Stack<Value> valueStack
    ) throws ExpressionException {
        Value operand = getOperands(valueStack)[0];
        try {
            valueStack.push(operand.copy(true));
        } catch (TypeException ex) {
            postValueDiagnostic(false, assembler.getDiagnostics());
            throw new ExpressionException();
        }
    }
}
