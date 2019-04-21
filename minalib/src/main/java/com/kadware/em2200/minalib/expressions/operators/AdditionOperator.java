/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Class for addition operator
 */
@SuppressWarnings("Duplicates")
public class AdditionOperator extends ArithmeticOperator {

    /**
     * Constructor
     * @param locale location of this operator
     */
    public AdditionOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public final int getPrecedence(
    ) {
        return 6;
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
        try {
            Value[] operands = getTransformedOperands(valueStack, true, diagnostics);
            Value opResult;

            if (operands[0].getType() == ValueType.Integer) {
                IntegerValue iopLeft = (IntegerValue)operands[0];
                IntegerValue iopRight = (IntegerValue)operands[1];
                if (iopLeft.getFlagged() || iopRight.getFlagged()) {
                    diagnostics.append(new ValueDiagnostic( getLocale(), "Cannot add flagged values" ));
                    throw new ExpressionException();
                }

                long intResult = iopLeft.getValue() + iopRight.getValue();

                List<IntegerValue.UndefinedReference> temp = new LinkedList<>();
                temp.addAll(Arrays.asList(iopLeft.getUndefinedReferences()));
                temp.addAll(Arrays.asList(iopRight.getUndefinedReferences()));
                Map<String, Integer> tallyMap = new HashMap<>();
                for (IntegerValue.UndefinedReference ref : temp) {
                    int refVal = ref._isNegative ? -1 : 1;
                    if ( tallyMap.containsKey( ref._reference ) ) {
                        refVal += tallyMap.get(ref._reference);
                    }
                    tallyMap.put(ref._reference, refVal);
                }

                List<IntegerValue.UndefinedReference> newRefs = new LinkedList<>();
                for (Map.Entry<String, Integer> entry : tallyMap.entrySet()) {
                    boolean isNeg = entry.getValue() < 0;
                    for (int ex = 0; ex < Math.abs(entry.getValue()); ++ex) {
                        newRefs.add(new IntegerValue.UndefinedReference(entry.getKey(), isNeg));
                    }
                }

                opResult = new IntegerValue(false, intResult, newRefs.toArray(new IntegerValue.UndefinedReference[0] ));
            } else {
                FloatingPointValue iopLeft = (FloatingPointValue)operands[0];
                FloatingPointValue iopRight = (FloatingPointValue)operands[1];
                double result = iopLeft.getValue() + iopRight.getValue();
                opResult = new FloatingPointValue(false, result);
            }

            valueStack.push(opResult);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
