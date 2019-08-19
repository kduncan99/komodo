/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.diagnostics.RelocationDiagnostic;
import com.kadware.komodo.minalib.exceptions.*;
import java.util.Stack;

/**
 * Class for multiplication operator
 */
@SuppressWarnings("Duplicates")
public class MultiplicationOperator extends ArithmeticOperator {

    /**
     * Constructor
     * @param locale location of operator
     */
    public MultiplicationOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public final int getPrecedence(
    ) {
        return 7;
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
        try {
            Value[] operands = getTransformedOperands(valueStack, true, context.getDiagnostics());
            Value opResult;

            if (operands[0].getType() == ValueType.Integer) {
                IntegerValue leftValue = operands[0].toIntegerValue(getLocale(), context.getDiagnostics());
                IntegerValue rightValue = operands[1].toIntegerValue(getLocale(), context.getDiagnostics());
                opResult = IntegerValue.multiply(leftValue, rightValue, getLocale(), context.getDiagnostics());
            } else {
                //  must be floating point
                FloatingPointValue leftValue = (FloatingPointValue)operands[0];
                FloatingPointValue rightValue = (FloatingPointValue)operands[1];
                double result = leftValue._value * rightValue._value;
                opResult = new FloatingPointValue(false, result);
            }

            valueStack.push(opResult);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
