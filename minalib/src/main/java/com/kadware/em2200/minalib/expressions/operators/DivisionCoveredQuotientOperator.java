/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
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

            IntegerValue leftValue = operands[0].toIntegerValue(getLocale(), context.getDiagnostics());
            if (leftValue._undefinedReferences.length != 0) {
                context.appendDiagnostic( new RelocationDiagnostic( getLocale() ) );
            }

            IntegerValue rightValue = operands[1].toIntegerValue(getLocale(), context.getDiagnostics());
            if (rightValue._undefinedReferences.length != 0) {
                context.appendDiagnostic( new RelocationDiagnostic( getLocale() ) );
            }

            if (rightValue._value == 0) {
                context.appendDiagnostic(new TruncationDiagnostic(getLocale(), "Division by zero"));
                throw new ExpressionException();
            }

            long result = leftValue._value % rightValue._value;
            valueStack.push( new IntegerValue( false, result, null ) );
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
