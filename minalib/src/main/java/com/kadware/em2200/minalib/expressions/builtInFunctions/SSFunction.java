/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.builtInFunctions;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import com.kadware.em2200.minalib.expressions.Expression;

/**
 * Produces a substring of the argument string
 */
@SuppressWarnings("Duplicates")
public class SSFunction extends BuiltInFunction {

    /**
     * Constructor
     * @param locale location of the function
     * @param argumentExpressions arguments
     */
    public SSFunction(
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
        return "$SS";
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public int getMaximumArguments(
    ) {
        return 3;
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
        try {
            Value[] arguments = evaluateArguments(context, diagnostics);
            if (arguments[0].getType() != ValueType.String) {
                diagnostics.append(getValueDiagnostic(1));
                throw new ExpressionException();
            }

            if (arguments[1].getType() != ValueType.Integer) {
                diagnostics.append(getValueDiagnostic(2));
                throw new ExpressionException();
            }

            if ((arguments.length == 3) && (arguments[2].getType() != ValueType.Integer)) {
                diagnostics.append(getValueDiagnostic(3));
                throw new ExpressionException();
            }

            StringValue sarg = (StringValue)arguments[0];
            IntegerValue iarg1 = (IntegerValue)arguments[1];
            IntegerValue iarg2 = (arguments.length == 3) ? (IntegerValue)arguments[2] : null;

            if (iarg1._undefinedReferences.length != 0) {
                diagnostics.append(new RelocationDiagnostic(getLocale()));
            }

            if ((iarg2 != null) && (iarg2._undefinedReferences.length != 0)) {
                diagnostics.append(new RelocationDiagnostic(getLocale()));
            }

            if (iarg1._value < 1) {
                diagnostics.append(new ValueDiagnostic(getLocale(), "Index argument must be > 0"));
                throw new ExpressionException();
            }

            if ((iarg2 != null) && (iarg2._value < 1)) {
                diagnostics.append(new ValueDiagnostic(getLocale(), "Count argument must be > 0"));
                throw new ExpressionException();
            }

            String sval = sarg._value;
            int ival1 = (int)iarg1._value;
            int ival2 = (iarg2 == null) ? sval.length() - ival1 : (int)iarg2._value;
            StringBuilder sb = new StringBuilder();
            sb.append((ival1 < sval.length()) ? sval.substring(ival1, ival2) : "");
            if (sb.length() < ival2) {
                do {
                    sb.append(' ');
                } while (sb.length() < ival2);
            }

            return new StringValue(false, sb.toString(), sarg._characterMode);
        } catch (ExpressionException ex) {
            diagnostics.append(new ErrorDiagnostic(getLocale(), ex.getMessage()));
            throw ex;
        }
    }
}
