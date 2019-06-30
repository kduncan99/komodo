/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.builtInFunctions;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.expressions.Expression;

/**
 * Converts the parameter to a string in the current character set
 */
public class SLFunction extends BuiltInFunction {

    /**
     * Constructor
     * @param locale location of this function
     * @param argumentExpressions argument expressions
     */
    public SLFunction(
        final Locale locale,
        final Expression[] argumentExpressions
    ) {
        super(locale, argumentExpressions);
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public String getFunctionName(
    ) {
        return "$SL";
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public int getMaximumArguments(
    ) {
        return 1;
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public int getMinimumArguments(
    ) {
        return 1;
    }

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
        if (arguments[0].getType() != ValueType.String) {
            context.appendDiagnostic(getValueDiagnostic(1));
            throw new ExpressionException();
        }

        StringValue sarg = (StringValue)arguments[0];
        return new IntegerValue(false, sarg._value.length(), null);
    }
}
