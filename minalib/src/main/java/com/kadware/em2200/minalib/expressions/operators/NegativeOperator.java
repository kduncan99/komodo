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
 * Class for negative (leading sign) operator
 */
public class NegativeOperator extends Operator {

    /**
     * Constructor
     * @param locale location of operator
     */
    public NegativeOperator(
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
        return 9;
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
        switch(operand.getType()) {
            case Integer:
                IntegerValue ioperand = (IntegerValue) operand;
                UndefinedReference[] opRefs = ioperand._undefinedReferences;
                UndefinedReference[] negRefs = new UndefinedReference[opRefs.length];
                for (int urx = 0; urx < opRefs.length; ++urx) {
                    negRefs[urx] = opRefs[urx].copy(!opRefs[urx]._isNegative);
                }
                IntegerValue iresult = new IntegerValue(ioperand._flagged, -ioperand._value, negRefs);
                valueStack.push(iresult);
                break;

            case FloatingPoint:
                FloatingPointValue fpoperand = (FloatingPointValue) operand;
                FloatingPointValue fpresult = new FloatingPointValue(fpoperand._flagged, -fpoperand._value);
                valueStack.push(fpresult);
                break;

            default:
                postValueDiagnostic(false, diagnostics);
                throw new ExpressionException();
        }
    }
}
