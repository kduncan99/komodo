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
public class SSFunction extends BuiltInFunction {

    /**
     * Constructor
     * <p>
     * @param locale
     * @param argumentExpressions
     */
    public SSFunction(
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
        return "$SS";
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public int getMaximumArguments(
    ) {
        return 3;
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

            if (iarg1.getRelocationInfo() != null) {
                diagnostics.append(new RelocationDiagnostic(getLocale()));
            }

            if ((iarg2 != null) && (iarg2.getRelocationInfo() != null)) {
                diagnostics.append(new RelocationDiagnostic(getLocale()));
            }

            String sval = sarg.getValue();
            long[] val1 = iarg1.getValue();
            long[] val2 = { 0, 0 };
            if (iarg2 != null) {
                val2 = iarg2.getValue();
            }

            if (OnesComplement.isNegative72(val1) || OnesComplement.isZero72(val1)) {
                diagnostics.append(new ValueDiagnostic(getLocale(), "Index argument must be >= 1"));
                throw new ExpressionException();
            }

            if (OnesComplement.isNegative72(val2)) {
                diagnostics.append(new ValueDiagnostic(getLocale(), "Count argument must be > 0"));
                throw new ExpressionException();
            }

            int ival1 = (int)val1[1];
            int ival2 = (int)val2[1];
            StringBuilder sb = new StringBuilder();
            sb.append((ival1 < sval.length()) ? sval.substring(ival1, ival2) : "");
            if (sb.length() < ival2) {
                do {
                    sb.append(' ');
                } while (sb.length() < ival2);
            }

            return new StringValue.Builder().setValue(sb.toString())
                                            .setCharacterMode(sarg.getCharacterMode())
                                            .build();
        } catch (ExpressionException ex) {
            diagnostics.append(new ErrorDiagnostic(getLocale(), ex.getMessage()));
            throw ex;
        }
    }
}
