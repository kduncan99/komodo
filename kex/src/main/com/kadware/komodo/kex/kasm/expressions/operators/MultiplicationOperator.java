/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.FloatingPointValue;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.dictionary.ValueType;
import com.kadware.komodo.kex.kasm.exceptions.*;
import java.util.Stack;

/**
 * Class for multiplication operator
 */
@SuppressWarnings("Duplicates")
public class MultiplicationOperator extends ArithmeticOperator {

    public MultiplicationOperator(Locale locale) { super(locale); }
    @Override public final int getPrecedence() { return 7; }

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
            Value[] operands = getTransformedOperands(valueStack, true, context.getDiagnostics());
            Value opResult;

            if (operands[0].getType() == ValueType.Integer) {
                //  both ops are integer
                IntegerValue iopLeft = (IntegerValue) operands[0];
                IntegerValue iopRight = (IntegerValue) operands[1];
                opResult = IntegerValue.multiply(iopLeft, iopRight, _locale, context.getDiagnostics());
            } else {
                //  both ops are floating point
                FloatingPointValue iopLeft = (FloatingPointValue)operands[0];
                FloatingPointValue iopRight = (FloatingPointValue)operands[1];
                opResult = FloatingPointValue.multiply(iopLeft, iopRight, _locale, context.getDiagnostics());
            }

            valueStack.push(opResult);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
