/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.diagnostics.TruncationDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.exceptions.*;
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
     * @param assembler context
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        final Assembler assembler,
        final Stack<Value> valueStack
    ) throws ExpressionException {
        try {
            Value[] operands = getTransformedOperands(valueStack, true, assembler);
            Value opResult;

            if (operands[0].getType() == ValueType.Integer) {
                IntegerValue iopLeft = (IntegerValue)operands[0];
                IntegerValue iopRight = (IntegerValue)operands[1];

                DoubleWord36.AdditionResult ar = iopLeft._value.add(iopRight._value.negate());
                if (ar._overflow) {
                    assembler.appendDiagnostic(new TruncationDiagnostic(_locale, "Addition overflow"));
                }

                //  Coalesce like-references, remove inverses, etc
                UnresolvedReference[] temp = new UnresolvedReference[iopLeft._references.length + iopRight._references.length];
                int tx = 0;
                for (int ix = 0; ix < iopLeft._references.length; ++tx, ++ix) {
                    temp[tx] = iopLeft._references[ix];
                }
                for (int ix = 0; ix < iopRight._references.length; ++tx, ++ix) {
                    temp[tx] = iopRight._references[ix].copy(!iopRight._references[ix]._isNegative);
                }

                UnresolvedReference[] coalesced = UnresolvedReference.coalesce(temp);
                opResult = new IntegerValue.Builder().setValue(ar._value)
                                                     .setReferences(coalesced)
                                                     .build();
            } else {
                FloatingPointValue iopLeft = (FloatingPointValue) operands[0];
                FloatingPointValue iopRight = (FloatingPointValue) operands[1];
                opResult = FloatingPointValue.add(iopLeft,
                                                  FloatingPointValue.negate(_locale, iopRight),
                                                  _locale,
                                                  assembler);
            }

            valueStack.push(opResult);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
