/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.items;

import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.dictionary.Value;

/**
 * expression item containing a Value object
 */
public class ValueItem extends OperandItem {

    public final Value _value;

    /**
     * Constructor
     * @param locale location of this item
     * @param value Value which we represent
     */
    public ValueItem(
        final Locale locale,
        final Value value
    ) {
        super(locale);
        _value = value;
    }

    /**
     * Resolves the value of this item - basically, we just return the Value object
     *
     * @param assembler@return a Value representing this operand
     */
    @Override
    public Value resolve(
        Assembler assembler) {
        return _value;
    }
}
