/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.minalib.expressions.operators.Operator;

/**
 * Allows us to stack an operator on the expression item stack
 */
public class OperatorItem implements ExpressionItem {

    private final Operator _operator;

    /**
     * Constructor
     * <p>
     * @param operator
     */
    public OperatorItem(
        final Operator operator
    ) {
        _operator = operator;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public Operator getOperator(
    ) {
        return _operator;
    }
}
