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
 * Class for logical OR operator
 */
public class OrOperator extends LogicalOperator {

    /**
     * Constructor
     * <p>
     * @param locale
     */
    public OrOperator(
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
        return 4;
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
            IntegerValue leftValue = operands[0].toIntegerValue(getLocale(), diagnostics);
            if (leftValue.getRelocationInfo() != null) {
                diagnostics.append(new RelocationDiagnostic(getLocale()));
            }

            IntegerValue rightValue = operands[0].toIntegerValue(getLocale(), diagnostics);
            if (rightValue.getRelocationInfo() != null) {
                diagnostics.append(new RelocationDiagnostic(getLocale()));
            }

            long[] result = new long[2];
            result[0] = leftValue.getValue()[0] | rightValue.getValue()[0];
            result[1] = leftValue.getValue()[1] | rightValue.getValue()[1];

            Signed signed = Signed.None;
            Precision precision = resolvePrecision(leftValue, rightValue);
            Form form = selectMatchingOrOnlyForm(leftValue, rightValue);

            valueStack.push(new IntegerValue.Builder().setValue(result)
                                                      .setSigned(signed)
                                                      .setPrecision(precision)
                                                      .setForm(form)
                                                      .build());
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
