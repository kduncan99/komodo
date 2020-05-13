/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.CharacterMode;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.StringValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
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
     * @param assembler
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        Assembler assembler, Stack<Value> valueStack) throws ExpressionException {
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
