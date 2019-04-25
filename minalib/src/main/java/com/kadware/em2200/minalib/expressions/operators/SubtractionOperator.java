/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.Locale;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;

import java.util.*;

/**
 * Class for subtraction operator
 */
@SuppressWarnings("Duplicates")
public class SubtractionOperator extends ArithmeticOperator {

    /**
     * Constructor
     * @param locale location of operator
     */
    public SubtractionOperator(
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

                long intResult = iopLeft._value + iopRight._value;

                List<IntegerValue.UndefinedReference> temp = new LinkedList<>();
                temp.addAll( Arrays.asList( iopLeft._undefinedReferences));
                for (IntegerValue.UndefinedReference ref : iopRight._undefinedReferences) {
                    temp.add(new IntegerValue.UndefinedReference( ref._reference, !ref._isNegative ));
                }

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
                double result = iopLeft._value - iopRight._value;
                opResult = new FloatingPointValue(false, result);
            }

            valueStack.push(opResult);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
