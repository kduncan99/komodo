/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
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
     * <p>
     * @param value Value which we represent
     */
    public ValueItem(
        final Value value
    ) {
        _value = value;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public Value getValue(
    ) {
        return _value;
    }

    /**
     * Resolves the value of this item - basically, we just return the Value object
     * <p>
     * @param context assembler context - we don't need this
     * @param diagnostics where we store any diagnostics we need to generate - we don't need this either
     * <p>
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
