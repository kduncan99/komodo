/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.expressions.items;

import com.kadware.komodo.kex.kasm.Locale;

/**
 * Base class for an expression item - basically, a component of which expressions are comprised
 */
public abstract class ExpressionItem {

    public final Locale _locale;

    ExpressionItem(
        Locale locale
    ) {
        _locale = locale;
    }
}
