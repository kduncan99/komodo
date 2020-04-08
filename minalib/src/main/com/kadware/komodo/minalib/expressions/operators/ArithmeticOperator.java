/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.diagnostics.Diagnostics;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.diagnostics.ValueDiagnostic;
import com.kadware.komodo.minalib.dictionary.FloatingPointValue;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.dictionary.ValueType;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.exceptions.TypeException;
import java.util.Stack;

/**
 * Base class for infix relational operators
 */
@SuppressWarnings("Duplicates")
public abstract class ArithmeticOperator extends Operator {

    ArithmeticOperator(Locale locale) { super(locale); }

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

        //  If at least one operator is floating point, then we make both of them floating point (if possible)
        if ((opType0 == ValueType.FloatingPoint) || (opType1 == ValueType.FloatingPoint)) {
            if (!allowFloatingPoint) {
                diagnostics.append(new ValueDiagnostic(_locale, "Floating point not allowed"));
                throw new TypeException();
            }

            if (opType0 != ValueType.FloatingPoint) {
                if (opType0 == ValueType.Integer) {
                    IntegerValue iv = (IntegerValue) operands[0];
                    operands[0] = FloatingPointValue.convertFromInteger(iv);
                } else {
                    diagnostics.append(new ValueDiagnostic(_locale, "Incompatible operands"));
                    throw new TypeException();
                }
            }

            if (opType1 != ValueType.FloatingPoint) {
                if (opType1 == ValueType.Integer) {
                    IntegerValue iv = (IntegerValue) operands[1];
                    operands[1] = FloatingPointValue.convertFromInteger(iv);
                } else {
                    diagnostics.append(new ValueDiagnostic(_locale, "Incompatible operands"));
                    throw new TypeException();
                }
            }

            return operands;
        }

        //  None of the operands are floating point - they'd better all be integer
        boolean error = false;
        if (opType0 != ValueType.Integer) {
            postValueDiagnostic(true, diagnostics);
            error = true;
        }

        if (opType1 != ValueType.Integer) {
            postValueDiagnostic(false, diagnostics);
            error = true;
        }

        if (error) {
            throw new TypeException();
        }

        return operands;
    }

    @Override public final Type getType() { return Type.Infix; }
}
