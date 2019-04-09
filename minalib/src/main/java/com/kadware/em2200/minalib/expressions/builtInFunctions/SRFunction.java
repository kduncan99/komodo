/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.builtInFunctions;

import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.Expression;

/**
 * Converts the parameter to a string in the current character set
 */
public class SRFunction extends BuiltInFunction {

    /**
     * Constructor
     * <p>
     * @param locale
     * @param argumentExpressions
     */
    public SRFunction(
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
        return "$SR";
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public int getMaximumArguments(
    ) {
        return 2;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public int getMinimumArguments(
    ) {
        return 2;
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

        if (arguments[1].getType() != ValueType.Integer) {
            diagnostics.append(getValueDiagnostic(2));
            throw new ExpressionException();
        }

        if (arguments[1].getRelocationInfo() != null) {
            diagnostics.append(new RelocationDiagnostic(getLocale()));
        }

        StringValue sarg = (StringValue)arguments[0];
        IntegerValue iarg = (IntegerValue)arguments[1];
        if (OnesComplement.isNegative72(iarg.getValue())) {
            diagnostics.append(new ValueDiagnostic(getLocale(), "Count argument cannot be negative"));
            throw new ExpressionException();
        }

        StringBuilder sb = new StringBuilder();
        for (int sx = 0; sx < (int)iarg.getValue()[1]; ++sx) {
            sb.append(sarg.getValue());
        }

        return new StringValue(sb.toString(),
                               false,
                               Signed.None,
                               Precision.None,
                               sarg.getCharacterMode());
    }
}
