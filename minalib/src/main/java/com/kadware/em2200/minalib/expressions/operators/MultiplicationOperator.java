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
     * @param diagnostics where we append diagnostics if necessary
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        final Context context,
        Stack<Value> valueStack,
        Diagnostics diagnostics
    ) throws ExpressionException {
        try {
            Value[] operands = getTransformedOperands(valueStack, true, diagnostics);
            Value opResult;

            if (operands[0].getType() == ValueType.Integer) {
                IntegerValue leftValue = operands[0].toIntegerValue(getLocale(), diagnostics);
                if (leftValue.getUndefinedReferences().length != 0) {
                    diagnostics.append( new RelocationDiagnostic( getLocale() ) );
                }
                if (leftValue.getFlagged()) {
                    diagnostics.append( new ValueDiagnostic( getLocale(), "Left operand cannot be flagged" ) );
                }

                IntegerValue rightValue = operands[0].toIntegerValue(getLocale(), diagnostics);
                if (rightValue.getUndefinedReferences().length != 0) {
                    diagnostics.append( new RelocationDiagnostic( getLocale() ) );
                }
                if (rightValue.getFlagged()) {
                    diagnostics.append( new ValueDiagnostic( getLocale(), "Right operand cannot be flagged" ) );
                }

                long result = leftValue.getValue() * rightValue.getValue();
                opResult = new IntegerValue( false, result, null );
            } else {
                //  must be floating point
                FloatingPointValue leftValue = (FloatingPointValue)operands[0];
                if (leftValue.getFlagged()) {
                    diagnostics.append( new ValueDiagnostic( getLocale(), "Left operand cannot be flagged" ) );
                }

                FloatingPointValue rightValue = (FloatingPointValue)operands[1];
                if (rightValue.getFlagged()) {
                    diagnostics.append( new ValueDiagnostic( getLocale(), "Right operand cannot be flagged" ) );
                }

                double result = leftValue.getValue() * rightValue.getValue();
                opResult = new FloatingPointValue( false, result );
            }

            valueStack.push( opResult );
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
