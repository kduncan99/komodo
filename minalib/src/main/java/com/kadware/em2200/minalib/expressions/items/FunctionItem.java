/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.items;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.dictionary.Value;
import com.kadware.em2200.minalib.exceptions.ExpressionException;

/**
 * Represents a function call within an expression
 */
public abstract class FunctionItem extends OperandItem {

    /**
     * constructor
     * @param locale where the item is found in the source code
     */
    FunctionItem(
        final Locale locale
    ) {
        super(locale);
    }

    /**
     * Evaluates the function against the parameter list
     * @param context assembly context
     * @return true if successful, false to discontinue evaluation
     * @throws ExpressionException if something is wrong with the expression
     */
    @Override
    public abstract Value resolve(
        final Context context
    ) throws ExpressionException;
}
