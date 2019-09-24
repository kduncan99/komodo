/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.minalib.dictionary.StringValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.dictionary.ValueJustification;
import com.kadware.komodo.minalib.dictionary.ValueType;
import com.kadware.komodo.minalib.exceptions.ExpressionException;

import java.util.Stack;

/**
 * Class for single precision operator
 */
public class RightJustificationOperator extends Operator {

    public RightJustificationOperator(Locale locale) { super(locale); }
    @Override public int getPrecedence() { return 10; }
    @Override public Type getType() { return Type.Postfix; }

    /**
     * Evaluator
     * @param context current contextual information one of our subclasses might need to know
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if evaluation fails
     */
    @Override
    public void evaluate(
        final Context context,
        Stack<Value> valueStack
    ) throws ExpressionException {
        Value operand = getOperands(valueStack)[0];
        if ( operand.getType() != ValueType.String) {
            context.appendDiagnostic(new ErrorDiagnostic(_locale, "Justification operator requires a string operand"));
            throw new ExpressionException();
        }

        valueStack.push(((StringValue) operand).copy(ValueJustification.Right));
    }
}
