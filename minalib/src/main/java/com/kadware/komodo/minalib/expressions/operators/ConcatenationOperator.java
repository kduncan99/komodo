/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.CharacterMode;
import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.dictionary.StringValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Class for string concatenation operator
 */
public class ConcatenationOperator extends Operator {

    public ConcatenationOperator(Locale locale) { super(locale); }
    @Override public int getPrecedence() { return 3; }
    @Override public Type getType() { return Type.Infix; }

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

        boolean error = false;
        if (!(operands[0] instanceof StringValue)) {
            postValueDiagnostic(true, context.getDiagnostics());
            error = true;
        }

        if (!(operands[1] instanceof StringValue)) {
            postValueDiagnostic(false, context.getDiagnostics());
            error = true;
        }

        if (error) {
            throw new ExpressionException();
        }

        StringValue leftValue = (StringValue) operands[0];
        StringValue rightValue = (StringValue) operands[1];
        String newValue = leftValue._value + rightValue._value;

        boolean ascii = (leftValue._characterMode == CharacterMode.ASCII)
                            || (rightValue._characterMode == CharacterMode.ASCII);
        CharacterMode charMode = ascii ? CharacterMode.ASCII : CharacterMode.Fieldata;
        valueStack.push(new StringValue.Builder().setValue(newValue).setCharacterMode(charMode).build());
    }
}
