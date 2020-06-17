/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.builtInFunctions;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.diagnostics.*;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;
import java.math.BigInteger;

/**
 * Produces a particular number of some character in the current character set
 */
@SuppressWarnings("Duplicates")
public class SRFunction extends BuiltInFunction {

    /**
     * Constructor
     * @param locale location of the function
     * @param argumentExpressions arguments
     */
    SRFunction(
        final Locale locale,
        final Expression[] argumentExpressions
    ) {
        super(locale, argumentExpressions);
    }

    @Override public String getFunctionName()   { return "$SR"; }
    @Override public int getMaximumArguments()  { return 2; }
    @Override public int getMinimumArguments()  { return 2; }

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

        if (arguments[1].getType() != ValueType.Integer) {
            assembler.appendDiagnostic(getValueDiagnostic(2));
            throw new ExpressionException();
        }

        StringValue sarg = (StringValue)arguments[0];
        IntegerValue iarg = (IntegerValue)arguments[1];
        if (iarg.hasUndefinedReferences()) {
            assembler.appendDiagnostic(new RelocationDiagnostic(getLocale()));
        }

        if (iarg.hasUndefinedReferences()) {
            assembler.appendDiagnostic(new ValueDiagnostic(getLocale(), "Count argument cannot be negative"));
            throw new ExpressionException();
        }

        if (iarg._value.get().compareTo(BigInteger.valueOf(0_777777)) > 0) {
            assembler.appendDiagnostic(new ValueDiagnostic(getLocale(), "Count argument cannot exceed 0700000"));
            throw new ExpressionException();
        }

        return new StringValue.Builder().setLocale(getLocale())
                                        .setValue(String.valueOf(sarg._value).repeat(Math.max(0, iarg._value.get().intValue())))
                                        .setCharacterMode(sarg._characterMode)
                                        .build();
    }
}
