/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.Word36;

/**
 * An extension of Word36 which describes a processor register.
 * Currently, there is no additional functionality nor attributes for a GeneralRegister over that of the base class.
 * Nevertheless, we'll leave this in place for the GeneralRegisterSet class, in case we think of something to add here, later.
 * More importantly, we serve as a superclass for IndexRegister, which *does* implement some additional functionality.
 */
public class GeneralRegister extends Word36 {

    /**
     * Standard constructor
     */
    public GeneralRegister() {}

    /**
     * Initial value constructor
     */
    public GeneralRegister(final long value) { super(value); }
}
