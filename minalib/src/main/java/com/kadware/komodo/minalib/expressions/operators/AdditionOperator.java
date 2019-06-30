/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.*;
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
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        final Context context,
        Stack<Value> valueStack
    ) throws ExpressionException {
        try {
            Value[] operands = getTransformedOperands(valueStack, true, context.getDiagnostics());
            Value opResult;

            if (operands[0].getType() == ValueType.Integer) {
                IntegerValue iopLeft = (IntegerValue)operands[0];
                IntegerValue iopRight = (IntegerValue)operands[1];

                long intResult = iopLeft._value + iopRight._value;

                //  Coalesce like-references, remove inverses, etc
                List<UndefinedReference> temp = new LinkedList<>();
                temp.addAll(Arrays.asList(iopLeft._undefinedReferences));
                temp.addAll(Arrays.asList(iopRight._undefinedReferences));

                //  Convert / Coalesce / Munge the UR's from the two operators into one set of UR's for the result
                List<UndefinedReference> newRefs = new LinkedList<>();

                //  Check for special collector symbols - such instances are indicated by temp containing
                //  exactly two entries - both of which are UndefinedReferenceToLabel, and the first of which
                //  is a known keyword.  If we find such a pair, convert them to a UndefinedReferenceSpecial entity.
                if ((temp.size() == 2)
                    && (temp.get(0) instanceof UndefinedReferenceToLabel)
                    && (temp.get(1) instanceof UndefinedReferenceToLabel)
                    && (temp.get(0)._fieldDescriptor.equals(temp.get(1)._fieldDescriptor))) {
                    UndefinedReferenceToLabel urLabel0 = (UndefinedReferenceToLabel) temp.get(0);
                    UndefinedReferenceToLabel urLabel1 = (UndefinedReferenceToLabel) temp.get(1);
                    UndefinedReferenceSpecial.Type type = UndefinedReferenceSpecial.TOKEN_TABLE.get(urLabel0._label);
                    if (type != null) {
                        UndefinedReferenceSpecial urSpecial =
                            new UndefinedReferenceSpecial(urLabel0._fieldDescriptor,
                                                          urLabel0._isNegative,
                                                          type,
                                                          urLabel1._label);
                        temp.clear();
                        newRefs.add(urSpecial);
                    }
                }

                if (newRefs.isEmpty()) {
                    Map<String, Integer> tallyLabels = new HashMap<>();
                    Map<Integer, Integer> tallyLCIndices = new HashMap<>();

                    for (UndefinedReference ref : temp) {
                        int refVal = ref._isNegative ? -1 : 1;
                        if (ref instanceof UndefinedReferenceToLabel) {
                            UndefinedReferenceToLabel lRef = (UndefinedReferenceToLabel) ref;
                            if (tallyLabels.containsKey(lRef._label)) {
                                refVal += tallyLabels.get(lRef._label);
                            }
                            tallyLabels.put(lRef._label, refVal);
                        } else if (ref instanceof UndefinedReferenceToLocationCounter) {
                            UndefinedReferenceToLocationCounter lRef = (UndefinedReferenceToLocationCounter) ref;
                            if (tallyLCIndices.containsKey(lRef._locationCounterIndex)) {
                                refVal += tallyLCIndices.get(lRef._locationCounterIndex);
                            }
                            tallyLCIndices.put(lRef._locationCounterIndex, refVal);
                        }
                    }

                    for (Map.Entry<String, Integer> entry : tallyLabels.entrySet()) {
                        if (entry.getValue() != 0) {
                            boolean isNeg = entry.getValue() < 0;
                            for (int ex = 0; ex < Math.abs(entry.getValue()); ++ex) {
                                newRefs.add(new UndefinedReferenceToLabel(new FieldDescriptor(0, 0),
                                                                          isNeg,
                                                                          entry.getKey()));
                            }
                        }
                    }
                    for (Map.Entry<Integer, Integer> entry : tallyLCIndices.entrySet()) {
                        if (entry.getValue() != 0) {
                            boolean isNeg = entry.getValue() < 0;
                            for (int ex = 0; ex < Math.abs(entry.getValue()); ++ex) {
                                newRefs.add(new UndefinedReferenceToLocationCounter(new FieldDescriptor(0, 0),
                                                                                    isNeg,
                                                                                    entry.getKey()));
                            }
                        }
                    }
                }

                opResult = new IntegerValue(false, intResult, newRefs.toArray(new UndefinedReference[0] ));
            } else {
                FloatingPointValue iopLeft = (FloatingPointValue)operands[0];
                FloatingPointValue iopRight = (FloatingPointValue)operands[1];
                double result = iopLeft._value + iopRight._value;
                opResult = new FloatingPointValue(false, result);
            }

            valueStack.push(opResult);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
