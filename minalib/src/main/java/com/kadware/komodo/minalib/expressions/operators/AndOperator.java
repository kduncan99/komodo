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
 * Class for logical AND operator
 */
@SuppressWarnings("Duplicates")
public class AndOperator extends LogicalOperator {

    /**
     * Constructor
     * @param locale location of operator
     */
    public AndOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public int getPrecedence(
    ) {
        return 5;
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
        Value[] operands = getOperands(valueStack);

        try {
            IntegerValue leftValue = operands[0].toIntegerValue(getLocale(), context.getDiagnostics());
            if (leftValue._undefinedReferences.length != 0) {
                context.appendDiagnostic( new RelocationDiagnostic(getLocale() ) );
            }
            if (leftValue._flagged) {
                context.appendDiagnostic( new ValueDiagnostic(getLocale(), "Left operand cannot be flagged" ) );
            }

            IntegerValue rightValue = operands[1].toIntegerValue(getLocale(), context.getDiagnostics());
            if (rightValue._undefinedReferences.length != 0) {
                context.appendDiagnostic( new RelocationDiagnostic( getLocale() ) );
            }
            if (rightValue._flagged) {
                context.appendDiagnostic( new ValueDiagnostic( getLocale(), "Right operand cannot be flagged" ) );
            }

            long result = leftValue._value & rightValue._value;
            valueStack.push( new IntegerValue(false, result, null) );
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
