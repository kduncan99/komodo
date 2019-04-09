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
public class SLFunction extends BuiltInFunction {

    /**
     * Constructor
     * <p>
     * @param locale
     * @param argumentExpressions
     */
    public SLFunction(
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
        return "$SL";
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
        Value[] arguments = evaluateArguments(context, diagnostics);
        if (arguments[0].getType() != ValueType.String) {
            diagnostics.append(getValueDiagnostic(1));
            throw new ExpressionException();
        }

        StringValue sarg = (StringValue)arguments[0];
        return new IntegerValue(sarg.getValue().length(), false, Signed.None, Precision.None, null, null);
    }
}
