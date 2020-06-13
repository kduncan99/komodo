/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.items;

import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.expressions.operators.Operator;

/**
 * Allows us to stack an operator on the expression item stack
 */
public class OperatorItem extends ExpressionItem {

    public final Operator _operator;

    /**
     * Constructor
     * @param operator operator object which we represent
     */
    public OperatorItem(
        final Operator operator
    ) {
        super(operator._locale);
        _operator = operator;
    }

    @Override
    public String toString() {
        return "OperatorItem:" + _operator.toString();
    }
}
