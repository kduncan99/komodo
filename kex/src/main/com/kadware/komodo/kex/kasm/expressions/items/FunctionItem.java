/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.items;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;

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
     *
     * @param assembler@return true if successful, false to discontinue evaluation
     * @throws ExpressionException if something is wrong with the expression
     */
    @Override
    public abstract Value resolve(
        final Assembler assembler
    ) throws ExpressionException;
}
