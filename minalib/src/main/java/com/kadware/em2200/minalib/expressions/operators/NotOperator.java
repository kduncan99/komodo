/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.operators;

import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.Stack;

/**
 * Class for negation operator
 */
public class NotOperator extends Operator {

    /**
     * Constructor
     * <p>
     * @param locale
     */
    public NotOperator(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public int getPrecedence(
    ) {
        return 1;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public Type getType(
    ) {
        return Type.Prefix;
    }

    /**
     * Evaluator
     * <p>
     * @param context current contextual information one of our subclasses might need to know
     * @param valueStack stack of values - we pop one or two from here, and push one back
     * @param diagnostics where we append diagnostics if necessary
     * <p>
     * @throws ExpressionException if something goes wrong with the process
     */
    @Override
    public void evaluate(
        final Context context,
        Stack<Value> valueStack,
        Diagnostics diagnostics
    ) throws ExpressionException {
        try {
            Value operand = getOperands(valueStack)[0];
            switch(operand.getType()) {
                case Integer:
                case FloatingPoint:
                case String:
                    IntegerValue interim = operand.toIntegerValue(getLocale(), diagnostics);
                    long newValue = OnesComplement.isZero72(interim.getValue()) ? 1 : 0;
                    valueStack.push(new IntegerValue(newValue,
                                                     false,
                                                     interim.getSigned(),
                                                     interim.getPrecision(),
                                                     interim.getForm(),
                                                     interim.getRelocationInfo()));
                    break;

                default:
                    postValueDiagnostic(false, diagnostics);
                    throw new TypeException();
            }
        } catch (TypeException ex) {
            postValueDiagnostic(false, diagnostics);
            throw new ExpressionException();
        }
    }
}
