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
 * Class for greater-than-or-equal operator
 */
public class GreaterOrEqualOperator extends RelationalOperator {

    /**
     * Constructor
     * <p>
     * @param locale
     */
    public GreaterOrEqualOperator(
        final Locale locale
    ) {
        super(locale);
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
        try {
            Value[] operands = getTransformedOperands(valueStack, diagnostics);
            int result = (operands[0].compareTo(operands[1]) >= 0) ? 1 : 0;
            valueStack.push(new IntegerValue.Builder().setValue(result)
                                                      .build());
        } catch (RelocationException ex) {
            //  thrown by compareTo() - we need to post a diag
            diagnostics.append(new RelocationDiagnostic(getLocale()));
            throw new ExpressionException();
        } catch (TypeException ex) {
            //  thrown by getTransformedOperands() - diagnostic already posted
            //  can be thrown by compareTo() - but won't be because we already prevented it in the previous call
            throw new ExpressionException();
        }
    }
}
