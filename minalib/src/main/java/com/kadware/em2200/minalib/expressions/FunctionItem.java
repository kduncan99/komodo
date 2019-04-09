/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.dictionary.Value;
import com.kadware.em2200.minalib.exceptions.ExpressionException;

/**
 * Represents a function call within an expression
 */
public abstract class FunctionItem extends OperandItem {

    /**
     * constructor
     */
    public FunctionItem(
    ) {
    }

    /**
     * Evaluates the function against the parameter list
     * <p>
     * @param context
     * @param diagnostics
     * <p>
     * @return true if successful, false to discontinue evaluation
     * <p>
     * @throws ExpressionException
     */
    @Override
    public abstract Value resolve(
        final Context context,
        Diagnostics diagnostics
    ) throws ExpressionException;
}
