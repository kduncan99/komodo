/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.exceptions.*;
import java.util.Stack;

/**
 * Base class for infix relational operators
 */
public abstract class ArithmeticOperator extends Operator {

    public ArithmeticOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Evaluator
     * @param context current contextual information one of our subclasses might need to know
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public abstract void evaluate(
        final Context context,
        Stack<Value> valueStack
    ) throws ExpressionException;

    /**
     * Wrapper around base class getOperands() method.
     * We call that method, then adjust the results before passing them up to the caller as follows:
     *      If the operands are of differing types:
     *          If either operand is floating point, the other is converted to floating point.
     *          Otherwise, anything not an integer is converted to integer.
     * If we find any invalid types, we post one or more diagnostics, and we throw TypeException.
     * @param valueStack the value stack from which we get the operators
     * @param allowFloatingPoint true to allow floating operands
     * @param diagnostics Diagnostics object to which we post any necessary diagnostics
     * @return left-hand possibly adjusted operand in result[0], right-hand in result[1]
     * @throws TypeException if either operand is other than floating point, integer, or string
     */
    protected Value[] getTransformedOperands(
        Stack<Value> valueStack,
        final boolean allowFloatingPoint,
        Diagnostics diagnostics
    ) throws TypeException {
        Value[] operands = super.getOperands(valueStack);
        ValueType opType0 = operands[0].getType();
        ValueType opType1 = operands[1].getType();

        if ((opType0 == ValueType.FloatingPoint) || (opType1 == ValueType.FloatingPoint)) {
            if (!allowFloatingPoint) {
                diagnostics.append(new ValueDiagnostic(getLocale(), "Floating point not allowed"));
                throw new TypeException();
            }

            if (opType0 != ValueType.FloatingPoint) {
                operands[0] = operands[0].toFloatingPointValue(getLocale(), diagnostics);
            }
            if (opType1 != ValueType.FloatingPoint) {
                operands[1] = operands[1].toFloatingPointValue(getLocale(), diagnostics);
            }

            return operands;
        }

        if (opType0 != ValueType.Integer) {
            operands[0] = operands[0].toIntegerValue(getLocale(), diagnostics);
        }
        if (opType1 != ValueType.Integer) {
            operands[1] = operands[1].toIntegerValue(getLocale(), diagnostics);
        }

        return operands;
    }

    @Override
    public final Type getType(
    ) {
        return Type.Infix;
    }
}
