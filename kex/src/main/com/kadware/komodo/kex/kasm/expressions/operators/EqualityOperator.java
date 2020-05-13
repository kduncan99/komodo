/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.exceptions.TypeException;
import java.util.Stack;

/**
 * Class for equality operator
 */
public class EqualityOperator extends RelationalOperator {

    public EqualityOperator(Locale locale) { super(locale); }

    /**
     * Evaluator
     * @param assembler
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        Assembler assembler, Stack<Value> valueStack) throws ExpressionException {
        try {
            Value[] operands = getTransformedOperands(valueStack, context.getDiagnostics());
            int result = (operands[0].equals(operands[1])) ? 1 : 0;
            valueStack.push(new IntegerValue.Builder().setValue(result).build());
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
