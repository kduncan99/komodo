/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;

import java.util.Stack;

/**
 * Class for negative (leading sign) operator
 */
public class NegativeOperator extends Operator {

    public NegativeOperator(Locale locale) { super(locale); }

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
                IntegerValue ioperand = (IntegerValue) operand;
                IntegerValue iresult = ioperand.negate();
                valueStack.push(iresult);
                break;

            case FloatingPoint:
                FloatingPointValue fpoperand = (FloatingPointValue) operand;
                FloatingPointValue fpresult = new FloatingPointValue.Builder().setValue(fpoperand._value.negate())
                                                                              .setPrecision(fpoperand._precision)
                                                                              .build();
                valueStack.push(fpresult);
                break;

            default:
                postValueDiagnostic(false, context.getDiagnostics());
                throw new ExpressionException();
        }
    }
}
