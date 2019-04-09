/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.builtInFunctions;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.Expression;

/**
 * Base class for built-in functions for expressions.
 */
public abstract class BuiltInFunction {

    private final Expression[] _argumentExpressions;
    private final Locale _locale;

    /**
     * Constructor
     * <p>
     * @param locale
     * @param argumentExpressions
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
     * <p>
     * @return
     */
    public abstract String getFunctionName();

    /**
     * Retrieves maximum number of arguments accepted from the subclass
     * <p>
     * @return
     */
    public abstract int getMaximumArguments();

    /**
     * Retrieves minimum number of arguments accepted from the subclass
     * <p>
     * @return
     */
    public abstract int getMinimumArguments();

    /**
     * Evaluator
     * <p>
     * @param context contextual information which might be needed by our subclasses
     * @param diagnostics where we post any appropriate diagnostics
     * <p>
     * @return Value object representing the result of the evaluation
     * <p>
     * @throws ExpressionException if something goes wrong with the evaluation process
     */
    public abstract Value evaluate(
        final Context context,
        final Diagnostics diagnostics
    ) throws ExpressionException;

    /**
     * Retrieves arguments for the function at evaluation time and evaluates them
     * <p>
     * @param context contextual information which might be needed by our subclasses
     * @param diagnostics where we post any necessary diagnostics
     * <p>
     * @return array of Value objects represented the evaluated argument expressions
     * <p>
     * @throws ExpressionException if the number of arguments specified in the code is invalid
     */
    public Value[] evaluateArguments(
        final Context context,
        final Diagnostics diagnostics
    ) throws ExpressionException {
        if ((_argumentExpressions.length < getMinimumArguments()) || (_argumentExpressions.length > getMaximumArguments())) {
            diagnostics.append(new ErrorDiagnostic(_locale, String.format("Wrong number of arguments for %s", getFunctionName())));
            throw new ExpressionException();
        }

        Value[] result = new Value[_argumentExpressions.length];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = _argumentExpressions[rx].evaluate(context, diagnostics);
        }
        return result;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    protected Locale getLocale(
    ) {
        return _locale;
    }

    /**
     * Convenience method to create a ValueDiagnostic given a bit of information
     * <p>
     * @param argumentNumber
     * <p>
     * @return created object
     */
    protected ValueDiagnostic getValueDiagnostic(
        final int argumentNumber
    ) {
        return new ValueDiagnostic(_locale, String.format("Bad argument value for arg %d for %s",
                                                          argumentNumber,
                                                          getFunctionName()));
    }
}
