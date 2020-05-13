/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.items;

import com.kadware.komodo.kex.kasm.expressions.operators.Operator;

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
