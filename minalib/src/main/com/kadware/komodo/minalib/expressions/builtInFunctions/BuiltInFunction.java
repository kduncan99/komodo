/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.builtInFunctions;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.expressions.Expression;

/**
 * Base class for built-in functions for expressions.
 */
public abstract class BuiltInFunction {

    private final Expression[] _argumentExpressions;
    private final Locale _locale;

    /**
     * constructor
     * @param locale where the item is found in the source code
     * @param argumentExpressions expressions representing the function arguments
     */
    public BuiltInFunction(
        final Locale locale,
        final Expression[] argumentExpressions
    ) {
        _locale = locale;
        _argumentExpressions = argumentExpressions;
    }

    /**
     * Retrieves the function name (including the leading '$' symbol) from the subclass
     * @return value
     */
    public abstract String getFunctionName();

    /**
     * Retrieves maximum number of arguments accepted from the subclass
     * @return value
     */
    public abstract int getMaximumArguments();

    /**
     * Retrieves minimum number of arguments accepted from the subclass
     * @return value
     */
    public abstract int getMinimumArguments();

    /**
     * Evaluator
     * @param context contextual information which might be needed by our subclasses
     * @return Value object representing the result of the evaluation
     * @throws ExpressionException if something goes wrong with the evaluation process
     */
    public abstract Value evaluate(
        final Context context
    ) throws ExpressionException;

    /**
     * Retrieves arguments for the function at evaluation time and evaluates them
     * @param context contextual information which might be needed by our subclasses
     * @return array of Value objects represented the evaluated argument expressions
     * @throws ExpressionException if the number of arguments specified in the code is invalid
     */
    Value[] evaluateArguments(
        final Context context
    ) throws ExpressionException {
        if ((_argumentExpressions.length < getMinimumArguments()) || (_argumentExpressions.length > getMaximumArguments())) {
            context.appendDiagnostic(new ErrorDiagnostic(_locale,
                                                         String.format("Wrong number of arguments for %s", getFunctionName())));
            throw new ExpressionException();
        }

        Value[] result = new Value[_argumentExpressions.length];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = _argumentExpressions[rx].evaluate(context);
        }
        return result;
    }

    /**
     * Getter
     * @return value
     */
    protected Locale getLocale(
    ) {
        return _locale;
    }

    /**
     * Convenience method to create a ValueDiagnostic given a bit of information
     * @param argumentNumber indicates which argument we refer to
     * @return created object
     */
    ValueDiagnostic getValueDiagnostic(
        final int argumentNumber
    ) {
        return new ValueDiagnostic(_locale, String.format("Bad argument value for arg %d for %s",
                                                          argumentNumber,
                                                          getFunctionName()));
    }
}
