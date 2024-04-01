/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

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
    public GeneralRegister(
    ) {
    }

    /**
     * Initial value constructor
     * <p>
     * @param value
     */
    public GeneralRegister(
        final long value
    ) {
        super(value);
    }
}
