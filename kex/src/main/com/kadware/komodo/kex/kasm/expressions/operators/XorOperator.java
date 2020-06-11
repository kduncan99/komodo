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
 * Class for logical XOR operator
 */
@SuppressWarnings("Duplicates")
public class XorOperator extends LogicalOperator {

    public XorOperator(Locale locale) { super(locale); }
    @Override public int getPrecedence() { return 4; }
    @Override public Type getType() { return Type.Infix; }

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
        IntegerValue leftValue = (IntegerValue) operands[0];
        IntegerValue rightValue = (IntegerValue) operands[1];
        IntegerValue result = IntegerValue.xor(leftValue, rightValue, _locale, assembler.getDiagnostics());
        valueStack.push(result);
    }
}
