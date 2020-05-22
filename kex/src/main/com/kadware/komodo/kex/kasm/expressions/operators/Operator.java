/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.diagnostics.Diagnostics;
import com.kadware.komodo.kex.kasm.diagnostics.ValueDiagnostic;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Base class for expression operators
 */
public abstract class Operator {

    public final Locale _locale;

    public enum Type {
        Infix,
        Prefix,
        Postfix,
    }

    public Operator(Locale locale) { _locale = locale; }

    /**
     * Evaluator
     * @param assembler context
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    public abstract void evaluate(
        final Assembler assembler,
        final Stack<Value> valueStack
    ) throws ExpressionException;

    /**
     * Retrieves the precedence for this operator.
     * higher values are evaluated before lower values.
     * We keep track of precedence here for convenience:
     *      0   Flagged
     *      1   Logical NOT
     *      2   All relational operators
     *      3   String Concatenation
     *      4   Logical OR
     *      5   Logical AND
     *      6   Addition
     *      6   Subtraction
     *      7   Division
     *      7   Division Covered Quotient
     *      7   Division
     *      7   Multiplication
     *      7   Shift operators
     *      8   '*''/' Fixed-point integer scaling (not implemented yet)
     *      8   '*+' Floating-point power of 10 positive scaling (not implemented yet)
     *      8   '*-' Floating-point power of 10 negative scaling (not implemented yet)
     *      8   '*''//' Floating-point power of 2 scaling (not implemented yet)
     *      9   Negate
     *      9   Positive
     *      10  Left/Right Justification
     *      10  Single/Double Precision
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
        final Stack<Value> valueStack
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

        throw new RuntimeException("Insufficient operands in valueStack Operator.getOperands()");
    }

    /**
     * Posts a ValueDiagnostic for the case where an operand is of the wrong type
     * @param leftOperand true if the offending operand is on the left of the operator; otherwise, on the right
     * @param diagnostics Diagnostics object to which the new Diagnostic is appended
     */
    void postValueDiagnostic(
        final boolean leftOperand,
        final Diagnostics diagnostics
    ) {
        String str = String.format("%s is of wrong type", leftOperand ? "left-hand" : "right-hand");
        diagnostics.append(new ValueDiagnostic(_locale, str));
    }
}
