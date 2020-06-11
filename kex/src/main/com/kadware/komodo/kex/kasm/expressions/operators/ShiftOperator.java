/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.diagnostics.FormDiagnostic;
import com.kadware.komodo.kex.kasm.diagnostics.RelocationDiagnostic;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.exceptions.*;
import java.math.BigInteger;
import java.util.Stack;

/**
 * Class for bit-shift operator
 */
public class ShiftOperator extends ArithmeticOperator {

    public ShiftOperator(Locale locale) { super(locale); }

    @Override public final int getPrecedence() { return 7; }

    /**
     * Evaluator
     * We do *NOT* flag T's on left shifts out of MSBit, contra MASM.
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

            IntegerValue iopLeft = (IntegerValue)operands[0];
            IntegerValue iopRight = (IntegerValue)operands[1];

            //  Forms are not allowed for either operand
            if ((iopLeft._form != null) || (iopRight._form != null)) {
                assembler.appendDiagnostic(new FormDiagnostic(_locale));
            }

            //  Undefined references not allowed for either operand
            if (iopLeft.hasUndefinedReferences() || iopRight.hasUndefinedReferences()) {
                assembler.appendDiagnostic(new RelocationDiagnostic(_locale));
            }

            BigInteger result = iopLeft._value.get();
            int count = iopRight._value.get().intValue();
            if (count < 0) {
                result = result.shiftRight(-count);
            } else {
                result = result.shiftLeft(count);
            }

            valueStack.push(new IntegerValue.Builder().setValue(new DoubleWord36(result)).build());
        } catch (TypeException ex) {
            throw new ExpressionException();
        }
    }
}
