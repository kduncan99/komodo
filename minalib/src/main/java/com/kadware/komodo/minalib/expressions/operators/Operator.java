/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.baselib.exceptions.InternalErrorRuntimeException;
import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.diagnostics.Diagnostics;
import com.kadware.komodo.minalib.diagnostics.ValueDiagnostic;
import com.kadware.komodo.minalib.exceptions.ExpressionException;

import java.util.Stack;

/**
 * Base class for expression operators
 */
public abstract class Operator {

    private final Locale _locale;

    public enum Type {
        Infix,
        Prefix,
        Postfix,
    }

    /**
     * Constructor
     * @param locale locale of the operator in the source code
     */
    public Operator(
        final Locale locale
    ) {
        _locale = locale;
    }

    /**
     * Evaluator
     * @param context current contextual information one of our subclasses might need to know
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    public abstract void evaluate(
        final Context context,
        Stack<Value> valueStack
    ) throws ExpressionException;

    /**
     * Getter
     * @return locale value
     */
    public Locale getLocale(
    ) {
        return _locale;
    }

    /**
     * Retrieves the precedence for this operator.
     * higher values are evaluated before lower values.
     * @return precedence
     */
    public abstract int getPrecedence();

    /**
     * Retrieves the type of this operator
     * @return type
     */
    public abstract Type getType();

    /**
     * Ensures we have the proper number of operands, and retrieves them.
     * For Infix, result[0] is left-hand operand and result[1] is right-hand operand
     * For Prefix and Postfix, result[0] is the only operand
     * @param valueStack stack of values
     * @return operand values
     */
    protected Value[] getOperands(
        Stack<Value> valueStack
    ) {
        if ((getType() == Type.Infix) && (valueStack.size() > 1)) {
                Value[] result = new Value[2];
                result[1] = valueStack.pop();
                result[0] = valueStack.pop();
                return result;
        } else if ((getType() != Type.Infix) && !valueStack.isEmpty()) {
            Value[] result = new Value[1];
            result[0] = valueStack.pop();
            return result;
        }

        throw new InternalErrorRuntimeException("Insufficient operands in valueStack Operator.getOperands()");
    }

    /**
     * Posts a ValueDiagnostic for the case where an operand is of the wrong type
     * <p>
     * @param leftOperand true if the offending operand is on the left of the operator; otherwise, on the right
     * @param diagnostics Diagnostics object to which the new Diagnostic is appended
     */
    void postValueDiagnostic(
        final boolean leftOperand,
        Diagnostics diagnostics
    ) {
        String str = String.format("%s is of wrong type", leftOperand ? "left-hand" : "right-hand");
        diagnostics.append(new ValueDiagnostic(_locale, str));
    }
}
