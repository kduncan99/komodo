/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.exceptions.*;

import java.util.Stack;

/**
 * Class for covered-quotient operator
 */
@SuppressWarnings("Duplicates")
public class DivisionCoveredQuotientOperator extends ArithmeticOperator {

    /**
     * Constructor
     * @param locale location of operator
     */
    public DivisionCoveredQuotientOperator(
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
            Value[] operands = getTransformedOperands(valueStack, false, context.getDiagnostics());
            IntegerValue iopLeft = (IntegerValue) operands[0];
            IntegerValue iopRight = (IntegerValue) operands[1];
            IntegerValue.DivisionResult dres = IntegerValue.divide(iopLeft, iopRight, getLocale(), context.getDiagnostics());
            valueStack.push(dres._coveredQuotient);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
