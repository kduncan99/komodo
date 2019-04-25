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
 * Class for string concatenation operator
 */
public class ConcatenationOperator extends Operator {

    /**
     * Constructor
     * @param locale location of the operator
     */
    public ConcatenationOperator(
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
        return 3;
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public Type getType(
    ) {
        return Type.Infix;
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
        Value[] operands = getOperands(valueStack);

        try {
            StringValue leftValue = operands[0].toStringValue(getLocale(), context._characterMode, diagnostics);
            StringValue rightValue = operands[1].toStringValue(getLocale(), context._characterMode, diagnostics);
            String newValue = leftValue._value + rightValue._value;

            boolean ascii = (leftValue._characterMode == CharacterMode.ASCII)
                                || (rightValue._characterMode == CharacterMode.ASCII);
            CharacterMode charMode = ascii ? CharacterMode.ASCII : CharacterMode.Fieldata;

            valueStack.push( new StringValue(false, newValue, charMode) );
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
