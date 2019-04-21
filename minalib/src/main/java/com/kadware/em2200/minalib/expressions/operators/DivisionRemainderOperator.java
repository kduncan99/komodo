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
 * Class for remainder operator
 */
@SuppressWarnings("Duplicates")
public class DivisionRemainderOperator extends ArithmeticOperator {

    /**
     * Constructor
     * @param locale locale of text
     */
    public DivisionRemainderOperator(
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
            Value[] operands = getTransformedOperands(valueStack, false, diagnostics);
            IntegerValue leftValue = operands[0].toIntegerValue(getLocale(), diagnostics);
            if (leftValue.getUndefinedReferences().length != 0) {
                diagnostics.append( new RelocationDiagnostic( getLocale() ) );
            }

            IntegerValue rightValue = operands[1].toIntegerValue(getLocale(), diagnostics);
            if (rightValue.getUndefinedReferences().length != 0) {
                diagnostics.append( new RelocationDiagnostic( getLocale() ) );
            }

            if (rightValue.getValue() == 0) {
                diagnostics.append(new TruncationDiagnostic(getLocale(), "Division by zero"));
                throw new ExpressionException();
            }

            //  do the math
            long result = leftValue.getValue() / rightValue.getValue();
            valueStack.push( new IntegerValue( false, result, null ) );
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
