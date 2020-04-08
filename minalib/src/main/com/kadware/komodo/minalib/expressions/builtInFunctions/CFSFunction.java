/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.builtInFunctions;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.IntegerValue;
import com.kadware.komodo.minalib.dictionary.StringValue;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.dictionary.ValuePrecision;
import com.kadware.komodo.minalib.exceptions.*;
import com.kadware.komodo.minalib.expressions.Expression;

/**
 * Converts the parameter to a string in the current character set
 */
@SuppressWarnings("Duplicates")
public class CFSFunction extends BuiltInFunction {

    /**
     * Constructor
     * @param locale location of the text for the function
     * @param argumentExpressions argument expressions
     */
    CFSFunction(
        final Locale locale,
        final Expression[] argumentExpressions
    ) {
        super(locale, argumentExpressions);
    }

    @Override public String getFunctionName()   { return "$CFS"; }
    @Override public int getMaximumArguments()  { return 1; }
    @Override public int getMinimumArguments()  { return 1; }

    /**
     * Evaluator
     * @param context evaluation-time contextual information
     * @return Value object representing the result of the evaluation
     * @throws ExpressionException if something goes wrong with the evaluation process
     */
    @Override
    public Value evaluate(
        final Context context
    ) throws ExpressionException {
        Value[] arguments = evaluateArguments(context);
        if (arguments[0] instanceof IntegerValue) {
            IntegerValue iv = (IntegerValue) arguments[0];
            if (iv._precision == ValuePrecision.Double) {
                return new StringValue.Builder().setValue(iv._value.toStringFromFieldata())
                                                .setCharacterMode(CharacterMode.Fieldata)
                                                .build();
            } else {
                String str = iv._value.getWords()[1].toStringFromFieldata();
                return new StringValue.Builder().setValue(str)
                                                .setCharacterMode(CharacterMode.Fieldata)
                                                .build();
            }
//            IntegerValue iv = (IntegerValue) arguments[0];
//            return new StringValue.Builder().setValue(iv._value.toStringFromFieldata())
//                                            .setCharacterMode(CharacterMode.Fieldata)
//                                            .build();
        } else {
            context.appendDiagnostic(getValueDiagnostic(1));
            throw new ExpressionException();
        }
    }
}
