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
 * Class for negation operator
 */
public class NotOperator extends Operator {

    /**
     * Constructor
     * @param locale location of operator
     */
    public NotOperator(
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
        return 1;
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public Type getType(
    ) {
        return Type.Prefix;
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
        Value operand = getOperands(valueStack)[0];
        if ( operand.getType() == ValueType.Integer ) {
            IntegerValue iop = (IntegerValue) operand;
            if (iop.getUndefinedReferences().length != 0) {
                diagnostics.append( new ValueDiagnostic( getLocale(),
                                                         "Not operator cannot be applied to integer with undefined references" ));
            }
            long ioperand = iop.getValue();
            long iresult = ioperand ^= 0_777777_777777L;
            valueStack.push( new IntegerValue(false, iresult, null ) );
        } else {
            postValueDiagnostic( false, diagnostics );
            throw new ExpressionException();
        }
    }
}
