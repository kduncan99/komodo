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
 * Converts the parameter to a string in the current character set
 */
public class CASFunction extends BuiltInFunction {

    /**
     * Constructor
     * <p>
     * @param locale
     * @param argumentExpressions
     */
    public CASFunction(
        final Locale locale,
        final Expression[] argumentExpressions
    ) {
        super(locale, argumentExpressions);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public String getFunctionName(
    ) {
        return "$CAS";
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public int getMaximumArguments(
    ) {
        return 1;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public int getMinimumArguments(
    ) {
        return 1;
    }

    /**
     * Evaluator
     * <p>
     * @param context evaluation-time contextual information
     * @param diagnostics where we append diagnostics if necessary
     * <p>
     * @return Value object representing the result of the evaluation
     * <p>
     * @throws ExpressionException if something goes wrong with the evaluation process
     */
    @Override
    public Value evaluate(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException {
        try {
            Value[] arguments = evaluateArguments(context, diagnostics);
            return arguments[0].toStringValue(getLocale(), CharacterMode.ASCII, diagnostics);
        } catch (TypeException ex) {
            diagnostics.append(this.getValueDiagnostic(1));
            throw new ExpressionException();
        }
    }
}
