/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.exceptions;

/**
 * Thrown when an operation is requested involving two different operands with non-equal forms
 */
public class FormException extends AssemblerException {

    public FormException() {
        super("Incompatible attached forms");
    }
}
