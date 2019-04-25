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
     * <p>
     * @param function
     */
    public BuiltInFunctionItem(
        final BuiltInFunction function
    ) {
        super(null);
        _function = function;
    }

    /**
     * Evaluates the function against its parameter list
     * <p>
     * @param context
     * @param diagnostics
     * <p>
     * @return true if successful, false to discontinue evaluation
     * <p>
     * @throws ExpressionException
     */
    @Override
    public Value resolve(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException {
        return _function.evaluate(context, diagnostics);
    }
}
