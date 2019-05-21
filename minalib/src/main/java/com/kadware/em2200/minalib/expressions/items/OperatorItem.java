/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions.items;

import com.kadware.em2200.minalib.expressions.operators.Operator;

/**
 * Allows us to stack an operator on the expression item stack
 */
public class OperatorItem implements IExpressionItem {

    public final Operator _operator;

    /**
     * Constructor
     * @param operator operator object which we represent
     */
    public OperatorItem(
        final Operator operator
    ) {
        _operator = operator;
    }
}
