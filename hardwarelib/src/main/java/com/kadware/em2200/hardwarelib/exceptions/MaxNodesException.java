/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.exceptions;

/**
 * Exception thrown when at attempt is made to create too many nodes of some particular class
 */
public class MaxNodesException extends Exception {

    public MaxNodesException(
        final Class clazz
    ) {
        super(String.format("Attempted to exceed the maximum number of nodes of type %s", clazz.getName()));
    }
}
