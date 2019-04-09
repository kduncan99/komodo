/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.Stack;

/**
 * Base class for infix relational operators
 */
public abstract class RelationalOperator extends Operator {

    /**
     * Constructor
     * <p>
     * @param locale
     */
    public RelationalOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Evaluator
     * <p>
     * @param context current contextual information one of our subclasses might need to know
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @param diagnostics where we append diagnostics if necessary
     * <p>
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public abstract void evaluate(
        final Context context,
        Stack<Value> valueStack,
        Diagnostics diagnostics
    ) throws ExpressionException;

    /**
     * Wrapper around base class getOperands() method.
     * We call that method, then adjust the results before passing them up to the caller as follows:
     *      If the operands are of differing types:
     *          If either operand is floating point, the other is converted to floating point.
     *          Else, if either operand is integer, the other is converted to integer.
     *      Else, the operands are returned unchanged
     * If we find any invalid types, we post one or more diagnostics, and we throw TypeException.
     * <p>
     * @param valueStack the value stack from which we get the operators
     * @param diagnostics Diagnostics object to which we post any necessary diagnostics
     * <p>
     * @return left-hand possibly adjusted operand in result[0], right-hand in result[1]
     * <p>
     * @throws TypeException if either operand is other than floating point, integer, or string
     */
    protected Value[] getTransformedOperands(
        Stack<Value> valueStack,
        Diagnostics diagnostics
    ) throws TypeException {
        Value[] operands = super.getOperands(valueStack);
        ValueType opType0 = operands[0].getType();
        ValueType opType1 = operands[1].getType();

        if (opType0 != opType1) {
            //  The types are not the same.  Conversions will automatically post diags and throw TypeExceptions
            if (opType0 == ValueType.FloatingPoint) {
                operands[1] = operands[1].toFloatingPointValue(getLocale(), diagnostics);
            } else if (opType1 == ValueType.FloatingPoint) {
                operands[0] = operands[0].toFloatingPointValue(getLocale(), diagnostics);
            } else if (opType0 == ValueType.Integer) {
                operands[1] = operands[1].toIntegerValue(getLocale(), diagnostics);
            } else if (opType1 == ValueType.Integer) {
                operands[0] = operands[0].toIntegerValue(getLocale(), diagnostics);
            } else {
                //  At this point, at most, one of the parameters is String, but at least one of them is bad.
                //  They're definitely not both strings, and neither is integer or float.
                if (opType0 != ValueType.String) {
                    postValueDiagnostic(true, diagnostics);
                }
                if (opType1 != ValueType.String) {
                    postValueDiagnostic(false, diagnostics);
                }
                throw new TypeException();
            }
        } else {
            //  The types are identical.  Check one side to make sure the type is valid.
            switch (operands[0].getType()) {
                case Integer:
                case FloatingPoint:
                case String:
                    break;

                default:
                    postValueDiagnostic(true, diagnostics);
                    postValueDiagnostic(false, diagnostics);
                    throw new TypeException();
            }
        }

        return operands;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public final int getPrecedence(
    ) {
        return 2;
    }

    /**
     * Retrieves the type of this operator
     * <p>
     * @return
     */
    @Override
    public final Type getType(
    ) {
        return Type.Infix;
    }
}
