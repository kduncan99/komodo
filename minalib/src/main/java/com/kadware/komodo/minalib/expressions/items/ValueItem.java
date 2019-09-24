/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.expressions.items;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.dictionary.Value;

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
     * @param context assembler context - we don't need this
     * @return a Value representing this operand
     */
    @Override
    public Value resolve(
        final Context context
    ) {
        return _value;
    }
}
