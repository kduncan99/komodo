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
 * Class for negation operator
 */
public class NotOperator extends LogicalOperator {

    public NotOperator(Locale locale) { super(locale); }
    @Override public int getPrecedence() { return 1; }
    @Override public Type getType() { return Type.Prefix; }

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
        Value[] operands = getOperands(valueStack, assembler);
        IntegerValue ioperand = (IntegerValue) operands[0];
        IntegerValue iresult = ioperand.negate();
        valueStack.push(iresult);
    }
}
