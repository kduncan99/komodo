/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.builtInFunctions;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.expressions.Expression;

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

        if (arguments[1].getType() != ValueType.Integer) {
            context.appendDiagnostic(getValueDiagnostic(2));
            throw new ExpressionException();
        }

        StringValue sarg = (StringValue)arguments[0];
        IntegerValue iarg = (IntegerValue)arguments[1];
        if (iarg.hasUndefinedReferences()) {
            context.appendDiagnostic(new RelocationDiagnostic(getLocale()));
        }

        if (iarg.hasUndefinedReferences()) {
            context.appendDiagnostic(new ValueDiagnostic(getLocale(), "Count argument cannot be negative"));
            throw new ExpressionException();
        }

        StringBuilder sb = new StringBuilder();
        for (int sx = 0; sx < iarg.getIntrinsicValue(); ++sx) {
            sb.append(sarg._value);
        }

        return new StringValue(false, sb.toString(), sarg._characterMode);
    }
}
