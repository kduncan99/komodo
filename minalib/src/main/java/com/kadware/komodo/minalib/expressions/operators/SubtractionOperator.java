/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.FloatingPointComponents;
import com.kadware.komodo.baselib.exceptions.CharacteristicOverflowException;
import com.kadware.komodo.baselib.exceptions.CharacteristicUnderflowException;
import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.diagnostics.ErrorDiagnostic;
import com.kadware.komodo.minalib.diagnostics.TruncationDiagnostic;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.minalib.exceptions.*;

import java.math.BigInteger;
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

                DoubleWord36.AdditionResult ar = iopLeft._value.add(iopRight._value.negate());
                if (ar._overflow) {
                    context.appendDiagnostic(new TruncationDiagnostic(_locale, "Addition overflow"));
                }

                //  Coalesce like-references, remove inverses, etc
                List<UndefinedReference> temp = new LinkedList<>();
                temp.addAll(Arrays.asList(iopLeft._references));
                temp.addAll(Arrays.asList(iopRight._references));

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

                List<UndefinedReference> newRefs = new LinkedList<>();
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

                opResult = new IntegerValue.Builder().setValue(ar._value)
                                                     .setReferences(newRefs.toArray(new UndefinedReference[0]))
                                                     .build();
            } else {
                FloatingPointValue iopLeft = (FloatingPointValue) operands[0];
                FloatingPointValue iopRight = (FloatingPointValue) operands[1];
                opResult = FloatingPointValue.add(iopLeft, FloatingPointValue.negate(iopRight), _locale, context.getDiagnostics());
            }

            valueStack.push(opResult);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
