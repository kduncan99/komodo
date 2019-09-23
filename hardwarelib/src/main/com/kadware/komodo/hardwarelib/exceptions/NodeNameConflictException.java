/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when InventoryManager is given a node with a name which is already held by some other node
 */
public class NodeNameConflictException extends Exception {

    public NodeNameConflictException(
        final String name
    ) {
        super(String.format("Node name conflict:%s", name));
    }
}
