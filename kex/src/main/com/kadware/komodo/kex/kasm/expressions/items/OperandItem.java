/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.items;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.dictionary.Value;
import com.kadware.komodo.kex.kasm.exceptions.ExpressionException;

/**
 * Base class for an expression item which represents an operand.
 * This could be a value, a built-in function reference, etc.
 */
public abstract class OperandItem extends ExpressionItem {

    /**
     * constructor
     * @param locale where the item is found in the source code
     */
    OperandItem(
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
    public abstract Value resolve(
        final Assembler assembler
    ) throws ExpressionException;
}
