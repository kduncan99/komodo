/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.expressions.builtInFunctions.BuiltInFunction;
import com.kadware.em2200.minalib.exceptions.*;

/**
 * Represents a built-in function call within an expression
 */
public class BuiltInFunctionItem extends FunctionItem {

    public final BuiltInFunction _function;

    /**
     * constructor
     * @param function of interest
     */
    public BuiltInFunctionItem(
        final BuiltInFunction function
    ) {
        super(null);
        _function = function;
    }

    /**
     * Evaluates the function against its parameter list
     * @param context
     * @return true if successful, false to discontinue evaluation
     * @throws ExpressionException if something is wrong with the expression
     */
    @Override
    public Value resolve(
        final Context context
    ) throws ExpressionException {
        return _function.evaluate(context);
    }
}
