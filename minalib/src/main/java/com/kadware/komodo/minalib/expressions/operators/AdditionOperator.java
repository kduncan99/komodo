/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.dictionary.FloatingPointValue;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.dictionary.ValueType;
import com.kadware.komodo.minalib.exceptions.*;
import java.util.Stack;

/**
 * Class for addition operator
 */
@SuppressWarnings("Duplicates")
public class AdditionOperator extends ArithmeticOperator {

    public AdditionOperator(Locale locale)  { super(locale); }
    public final int getPrecedence()        { return 6; }

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
                //  both ops are integer
                IntegerValue iopLeft = (IntegerValue) operands[0];
                IntegerValue iopRight = (IntegerValue) operands[1];
                opResult = IntegerValue.add(iopLeft, iopRight, _locale, context.getDiagnostics());
            } else {
                //  both ops are floating point
                FloatingPointValue iopLeft = (FloatingPointValue)operands[0];
                FloatingPointValue iopRight = (FloatingPointValue)operands[1];
                opResult = FloatingPointValue.add(iopLeft, iopRight, _locale, context.getDiagnostics());
            }

            valueStack.push(opResult);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
