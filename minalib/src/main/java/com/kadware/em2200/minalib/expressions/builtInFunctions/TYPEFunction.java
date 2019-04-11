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
public class TYPEFunction extends BuiltInFunction {

    /**
     * Constructor
     * <p>
     * @param locale
     * @param argumentExpressions
     */
    public TYPEFunction(
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
        return "$TYPE";
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
        int iType = 0;
        switch (arguments[0].getType()) {
            case Integer:           iType = 1; break;
            case FloatingPoint:     iType = 2; break;
            case String:            iType = 3; break;
            case Node:              iType = 4; break;
            case InternalName:      iType = 5; break;
            case ProcName:          iType = 6; break;
            case FuncName:          iType = 7; break;
            case Directive:         iType = 8; break;
            case BuiltInFunction:   iType = 9; break;
        }

        return new IntegerValue.Builder().setValue(iType)
                                         .build();
    }
}
