/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.FormDiagnostic;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.*;
import java.util.Stack;

/**
 * Class for addition operator
 */
@SuppressWarnings("Duplicates")
public class AdditionOperator extends ArithmeticOperator {

    /**
     * Constructor
     * @param locale location of this operator
     */
    public AdditionOperator(
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
        return 6;
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
                IntegerValue iopLeft = (IntegerValue) operands[0];
                IntegerValue iopRight = (IntegerValue) operands[1];
                opResult = IntegerValue.add(iopLeft, iopRight, getLocale(), context.getDiagnostics());
            } else {
                FloatingPointValue iopLeft = (FloatingPointValue)operands[0];
                FloatingPointValue iopRight = (FloatingPointValue)operands[1];
                double result = iopLeft._value + iopRight._value;
                opResult = new FloatingPointValue(false, result);
            }

            valueStack.push(opResult);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
