/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib.exceptions;

/**
 * Exception thrown by a method when it is asked to load a binary module (such as an AbsoluteModule)
 * and cannot do so due to some problem with the module.
 */
public class BinaryLoadException extends Exception {

    public BinaryLoadException(
        final String name,
        final String message
    ) {
        super("Cannot load module '" + name + "':" + message);
    }
}
