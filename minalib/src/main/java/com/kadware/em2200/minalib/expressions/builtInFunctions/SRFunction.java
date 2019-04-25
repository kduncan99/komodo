/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.builtInFunctions;

import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.Expression;

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
    public SRFunction(
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
        return "$SR";
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public int getMaximumArguments(
    ) {
        return 2;
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public int getMinimumArguments(
    ) {
        return 2;
    }

    /**
     * Evaluator
     * @param context evaluation-time contextual information
     * @param diagnostics where we append diagnostics if necessary
     * @return Value object representing the result of the evaluation
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

        if (arguments[1].getType() != ValueType.Integer) {
            diagnostics.append(getValueDiagnostic(2));
            throw new ExpressionException();
        }

        StringValue sarg = (StringValue)arguments[0];
        IntegerValue iarg = (IntegerValue)arguments[1];
        if (iarg._undefinedReferences.length != 0) {
            diagnostics.append(new RelocationDiagnostic(getLocale()));
        }

        if (iarg._value < 0) {
            diagnostics.append(new ValueDiagnostic(getLocale(), "Count argument cannot be negative"));
            throw new ExpressionException();
        }

        StringBuilder sb = new StringBuilder();
        for (int sx = 0; sx < iarg._value; ++sx) {
            sb.append(sarg._value);
        }

        return new StringValue(false, sb.toString(), sarg._characterMode);
    }
}
