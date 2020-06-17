/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.builtInFunctions;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.dictionary.IntegerValue;
import com.kadware.komodo.kex.kasm.dictionary.StringValue;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.dictionary.ValuePrecision;
import com.kadware.komodo.kex.kasm.exceptions.*;
import com.kadware.komodo.kex.kasm.expressions.Expression;

/**
 * Converts the parameter to a string in the current character set
 */
@SuppressWarnings("Duplicates")
public class CASFunction extends BuiltInFunction {

    /**
     * Constructor
     * @param locale location of the text for the function
     * @param argumentExpressions argument expressions
     */
    CASFunction(
        final Locale locale,
        final Expression[] argumentExpressions
    ) {
        super(locale, argumentExpressions);
    }

    @Override public String getFunctionName()   { return "$CAS"; }
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
        if (arguments[0] instanceof IntegerValue) {
            IntegerValue iv = (IntegerValue) arguments[0];
            if (iv._precision == ValuePrecision.Double) {
                return new StringValue.Builder().setLocale(getLocale())
                                                .setValue(iv._value.toStringFromASCII())
                                                .setCharacterMode(CharacterMode.ASCII)
                                                .build();
            } else {
                String str = iv._value.getWords()[1].toStringFromASCII();
                return new StringValue.Builder().setLocale(getLocale())
                                                .setValue(str)
                                                .setCharacterMode(CharacterMode.ASCII)
                                                .build();
            }
        } else {
            assembler.appendDiagnostic(getValueDiagnostic(1));
            throw new ExpressionException();
        }
    }
}
