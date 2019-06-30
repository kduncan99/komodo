/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.*;
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
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        final Context context,
        Stack<Value> valueStack
    ) throws ExpressionException {
        Value[] operands = getOperands(valueStack);

        try {
            StringValue leftValue = operands[0].toStringValue(getLocale(), context.getCharacterMode(), context.getDiagnostics());
            StringValue rightValue = operands[1].toStringValue(getLocale(), context.getCharacterMode(), context.getDiagnostics());
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
