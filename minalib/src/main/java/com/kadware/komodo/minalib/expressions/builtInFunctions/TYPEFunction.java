/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.builtInFunctions;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.*;
import com.kadware.komodo.minalib.exceptions.ExpressionException;
import com.kadware.komodo.minalib.expressions.Expression;

/**
 * Converts the parameter to a string in the current character set
 */
public class TYPEFunction extends BuiltInFunction {

    /**
     * Constructor
     * @param locale location of function ref
     * @param argumentExpressions arguments
     */
    public TYPEFunction(
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
        return "$TYPE";
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public int getMaximumArguments(
    ) {
        return 1;
    }

    /**
     * Getter
     * @return value
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
        Value[] arguments = evaluateArguments(context);
        int iType = 0;
        switch (arguments[0].getType()) {
            case Integer:           iType = 1; break;
            case FloatingPoint:     iType = 2; break;
            case String:            iType = 3; break;
            case Node:              iType = 4; break;
            case InternalName:      iType = 5; break;
            case Procedure:         iType = 6; break;
            case UserFunction:      iType = 7; break;
            case Directive:         iType = 8; break;
            case BuiltInFunction:   iType = 9; break;
            case Form:              iType = 10; break;  //  this one is non-standard
        }

        return new IntegerValue(false, iType, null);
    }
}
