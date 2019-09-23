/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.operators;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.diagnostics.TruncationDiagnostic;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.*;
import java.util.Stack;

/**
 * Class for subtraction operator
 */
@SuppressWarnings("Duplicates")
public class SubtractionOperator extends ArithmeticOperator {

    public SubtractionOperator(Locale locale) { super(locale); }

    @Override public final int getPrecedence() { return 6; }

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
                UndefinedReference[] temp = new UndefinedReference[iopLeft._references.length + iopRight._references.length];
                int tx = 0;
                for (int ix = 0; ix < iopLeft._references.length; ++tx, ++ix) {
                    temp[tx] = iopLeft._references[ix];
                }
                for (int ix = 0; ix < iopRight._references.length; ++tx, ++ix) {
                    temp[tx] = iopRight._references[ix].copy(!iopRight._references[ix]._isNegative);
                }

                UndefinedReference[] coalesced = UndefinedReference.coalesce(temp);
                opResult = new IntegerValue.Builder().setValue(ar._value)
                                                     .setReferences(coalesced)
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
