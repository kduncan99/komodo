/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.exceptions.TypeException;
import java.util.Stack;

/**
 * Class for remainder operator
 */
@SuppressWarnings("Duplicates")
public class DivisionRemainderOperator extends ArithmeticOperator {

    public DivisionRemainderOperator(Locale locale) { super(locale); }
    @Override public final int getPrecedence() { return 7; }

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
            Value[] operands = getTransformedOperands(valueStack, false, assembler.getDiagnostics());
            IntegerValue iopLeft = (IntegerValue) operands[0];
            IntegerValue iopRight = (IntegerValue) operands[1];
            IntegerValue.DivisionResult dres = IntegerValue.divide(iopLeft, iopRight, _locale, assembler.getDiagnostics());
            valueStack.push(dres._remainder);
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
