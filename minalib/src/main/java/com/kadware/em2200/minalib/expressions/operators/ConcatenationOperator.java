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
 * Class for string concatenation operator
 */
public class ConcatenationOperator extends Operator {

    /**
     * Constructor
     * <p>
     * @param locale
     */
    public ConcatenationOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public int getPrecedence(
    ) {
        return 3;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public Type getType(
    ) {
        return Type.Infix;
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
    public void evaluate(
        final Context context,
        Stack<Value> valueStack,
        Diagnostics diagnostics
    ) throws ExpressionException {
        Value[] operands = getOperands(valueStack);

        try {
            StringValue leftValue = operands[0].toStringValue(getLocale(), context._characterMode, diagnostics);
            StringValue rightValue = operands[1].toStringValue(getLocale(), context._characterMode, diagnostics);
            String newValue = leftValue.getValue() + rightValue.getValue();

            boolean ascii = (leftValue.getCharacterMode() == CharacterMode.ASCII)
                                || (rightValue.getCharacterMode() == CharacterMode.ASCII);
            CharacterMode charMode = ascii ? CharacterMode.ASCII : CharacterMode.Fieldata;

            Precision precision = resolvePrecision(leftValue, rightValue);
            valueStack.push(new StringValue.Builder().setValue(newValue)
                                                     .setCharacterMode(charMode)
                                                     .build());
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
