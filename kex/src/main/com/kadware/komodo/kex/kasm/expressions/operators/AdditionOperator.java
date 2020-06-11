/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.FloatingPointValue;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.dictionary.ValueType;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.exceptions.TypeException;
import java.util.Stack;

/**
 * Class for addition operator
 */
@SuppressWarnings("Duplicates")
public class AdditionOperator extends ArithmeticOperator {

    public AdditionOperator(Locale locale)  { super(locale); }
    public final int getPrecedence()        { return 6; }

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
            Value[] operands = getTransformedOperands(valueStack, true, assembler.getDiagnostics());
            Value opResult;

            if (operands[0].getType() == ValueType.Integer) {
                //  both ops are integer
                IntegerValue iopLeft = (IntegerValue) operands[0];
                IntegerValue iopRight = (IntegerValue) operands[1];
                opResult = IntegerValue.add(iopLeft, iopRight, _locale, assembler.getDiagnostics());
            } else {
                //  both ops are floating point
                FloatingPointValue iopLeft = (FloatingPointValue)operands[0];
                FloatingPointValue iopRight = (FloatingPointValue)operands[1];
                opResult = FloatingPointValue.add(iopLeft, iopRight, _locale, assembler.getDiagnostics());
            }

            valueStack.push(opResult);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
