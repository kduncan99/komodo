/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.builtInFunctions;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.dictionary.*;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;
import com.kadware.komodo.kex.kasm.expressions.Expression;

/**
 * Converts the parameter to a string in the current character set
 */
public class TYPEFunction extends BuiltInFunction {

    /**
     * Constructor
     * @param locale location of function ref
     * @param argumentExpressions arguments
     */
    TYPEFunction(
        final Locale locale,
        final Expression[] argumentExpressions
    ) {
        super(locale, argumentExpressions);
    }

    @Override public String getFunctionName()   { return "$TYPE"; }
    @Override public int getMaximumArguments()  { return 1; }
    @Override public int getMinimumArguments()  { return 1; }

    /**
     * Evaluator
     * @param assembler evaluation-time contextual information
     * @return Value object representing the result of the evaluation
     * @throws ExpressionException if something goes wrong with the evaluation process
     */
    @Override
    public Value evaluate(
        final Assembler assembler
    ) throws ExpressionException {
        Value[] arguments = evaluateArguments(assembler);
        int iType = switch (arguments[0].getType()) {
            case Integer -> 1;
            case FloatingPoint -> 2;
            case String -> 3;
            case Node -> 4;
            case InternalName -> 5;
            case Procedure -> 6;
            case UserFunction -> 7;
            case Directive -> 8;
            case BuiltInFunction -> 9;
            case Form -> 10;    //  this one is non-standard
            case Equf -> 11;    //  this one too
        };

        return new IntegerValue.Builder().setValue(iType).build();
    }
}
