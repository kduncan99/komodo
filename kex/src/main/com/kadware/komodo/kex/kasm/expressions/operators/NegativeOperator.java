/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.operators;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.dictionary.FloatingPointValue;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import java.util.Stack;

/**
 * Class for negative (leading sign) operator
 */
public class NegativeOperator extends Operator {

    public NegativeOperator(Locale locale) { super(locale); }

    @Override public int getPrecedence() { return 9; }
    @Override public Type getType() { return Type.Prefix; }

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
        Value operand = getOperands(valueStack)[0];
        switch (operand.getType()) {
            case Integer -> {
                IntegerValue ioperand = (IntegerValue) operand;
                IntegerValue iresult = ioperand.negate(_locale);
                valueStack.push(iresult);
            }
            case FloatingPoint -> {
                FloatingPointValue fpoperand = (FloatingPointValue) operand;
                FloatingPointValue fpresult = new FloatingPointValue.Builder().setLocale(_locale)
                                                                              .setValue(fpoperand._value.negate())
                                                                              .setPrecision(fpoperand._precision)
                                                                              .build();
                valueStack.push(fpresult);
            }
            default -> {
                postValueDiagnostic(false, assembler);
                throw new ExpressionException();
            }
        }
    }
}
