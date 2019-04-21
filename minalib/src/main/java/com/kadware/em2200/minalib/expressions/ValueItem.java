/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.expressions;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.dictionary.Value;

/**
 * expression item containing a Value object
 */
public class ValueItem extends OperandItem {

    private final Value _value;

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
     * Getter
     * @return ValueItem object associated with this item
     */
    public Value getValue(
    ) {
        return _value;
    }

    /**
     * Resolves the value of this item - basically, we just return the Value object
     * @param context assembler context - we don't need this
     * @param diagnostics where we store any diagnostics we need to generate - we don't need this either
     * @return a Value representing this operand
     */
    @Override
    public Value resolve(
        final Context context,
        Diagnostics diagnostics
    ) {
        return _value;
    }
}
