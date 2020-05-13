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
 * Class for logical AND operator
 */
@SuppressWarnings("Duplicates")
public class AndOperator extends LogicalOperator {

    public AndOperator(Locale locale) { super(locale); }
    @Override public int getPrecedence() { return 5; }
    @Override public Type getType() { return Type.Infix; }

    /**
     * Evaluator
     * @param assembler
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        Assembler assembler, Stack<Value> valueStack) throws ExpressionException {
        Value[] operands = getOperands(valueStack, context);
        IntegerValue leftValue = (IntegerValue) operands[0];
        IntegerValue rightValue = (IntegerValue) operands[1];
        IntegerValue result = IntegerValue.and(leftValue, rightValue, _locale, context.getDiagnostics());
        valueStack.push(result);
    }
}
