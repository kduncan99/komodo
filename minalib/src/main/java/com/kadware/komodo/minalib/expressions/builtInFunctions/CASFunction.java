/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.builtInFunctions;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.Value;
import com.kadware.komodo.minalib.exceptions.*;
import com.kadware.komodo.minalib.expressions.Expression;

/**
 * Converts the parameter to a string in the current character set
 */
public class CASFunction extends BuiltInFunction {

    /**
     * Constructor
     * @param locale location of the text for the function
     * @param argumentExpressions argument expressions
     */
    public CASFunction(
        final Locale locale,
        final Expression[] argumentExpressions
    ) {
        super(locale, argumentExpressions);
    }

    /**
     * Getter
     * @return the function name
     */
    @Override
    public String getFunctionName(
    ) {
        return "$CAS";
    }

    /**
     * Getter
     * @return max arguments we expect
     */
    @Override
    public int getMaximumArguments(
    ) {
        return 1;
    }

    /**
     * Getter
     * @return min arguments we expect
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
        try {
            Value[] arguments = evaluateArguments(context);
            return arguments[0].toStringValue(getLocale(), CharacterMode.ASCII, context.getDiagnostics());
        } catch (TypeException ex) {
            context.appendDiagnostic(this.getValueDiagnostic(1));
            throw new ExpressionException();
        }
    }
}
