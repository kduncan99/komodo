/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.builtInFunctions;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;

/**
 * Converts the parameter to a string in the current character set
 */
public class SLFunction extends BuiltInFunction {

    /**
     * Constructor
     * @param locale location of this function
     * @param argumentExpressions argument expressions
     */
    SLFunction(
        final Locale locale,
        final Expression[] argumentExpressions
    ) {
        super(locale, argumentExpressions);
    }

    @Override public String getFunctionName()   { return "$SL"; }
    @Override public int getMaximumArguments()  { return 1; }
    @Override public int getMinimumArguments()  { return 1; }

    /**
     * Evaluator
     * @param assembler evaluation-time contextual information
     * @return Value object representing the result of the evaluation
     * @throws ExpressionException if something goes wrong with the evaluation process
     */
    @Override
    public Value evaluate(
        final Assembler assembler
    ) throws ExpressionException {
        Value[] arguments = evaluateArguments(assembler);
        if (arguments[0].getType() != ValueType.String) {
            assembler.appendDiagnostic(getValueDiagnostic(1));
            throw new ExpressionException();
        }

        StringValue sarg = (StringValue) arguments[0];
        return new IntegerValue.Builder().setLocale(getLocale())
                                         .setValue(sarg._value.length())
                                         .build();
    }
}
